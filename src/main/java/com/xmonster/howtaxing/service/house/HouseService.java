package com.xmonster.howtaxing.service.house;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.house.*;
import com.xmonster.howtaxing.dto.house.HouseListSearchResponse.HouseSimpleInfoResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenAuthResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseResultInfo;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse.HyphenUserResidentRegistrationData;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse.HyphenUserResidentRegistrationData.ChangeHistory;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAdrResponse;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAdrResponse.Results.JusoDetail;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListResponse.HyphenCommon;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListResponse.HyphenData.*;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.type.ErrorCode;

import com.xmonster.howtaxing.utils.HouseUtil;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HouseService {

    private final HyphenService hyphenService;
    private final JusoGovService jusoGovService;
    private final HouseAddressService houseAddressService;

    private final HouseRepository houseRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;

    private static final int MAX_JUSO_CALL_CNT = 3; // 주소 한건 당 주소기반산업지원서비스 도로명주소 재조회 호출 건수 최대값

    // 보유주택 조회(하이픈-청약홈)
    public Object getHouseListSearch(HouseListSearchRequest houseListSearchRequest) {
        log.info(">> [Service]HouseService getHouseListSearch - 보유주택 조회(하이픈-청약홈)");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        // 1. 하이픈 Access Token 가져오기
        HyphenAuthResponse hyphenAuthResponse = hyphenService.getAccessToken()
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, "하이픈에서 AccessToken을 가져오는데 실패했습니다."));
        String accessToken = hyphenAuthResponse.getAccess_token();

        // 2. 하이픈 주택소유정보 조회 호출
        HyphenUserHouseListResponse hyphenUserHouseListResponse = hyphenService.getUserHouseInfo(accessToken, houseListSearchRequest)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR));

        // 3. 하이픈 보유주택조회 결과가 정상인지 체크하여, 정상인 경우 조회 결과를 정리하여 별도 DTO에 저장
        //List<HyphenUserHouseResultInfo> hyphenUserHouseResultInfoList = null;
        List<House> houseList = new ArrayList<>();
        HyphenCommon hyphenCommon = hyphenUserHouseListResponse.getHyphenCommon();
        List<DataDetail1> list1 = hyphenUserHouseListResponse.getHyphenData().getList1();
        List<DataDetail2> list2 = hyphenUserHouseListResponse.getHyphenData().getList2();
        List<DataDetail3> list3 = hyphenUserHouseListResponse.getHyphenData().getList3();

        if(this.isSuccessHyphenUserHouseListResponse(hyphenCommon)){
            // 하이픈 보유주택조회 결과 List를 HouseList에 세팅(3->1->2 순서로 호출)
            this.setList3ToHouseEntity(list3, houseList);
            this.setList1ToHouseEntity(list1, houseList);
            this.setList2ToHouseEntity(list2, houseList);

            // 전체 보유주택에 사용자id 세팅
            for(House house : houseList){
                house.setUserId(findUser.getId());
            }

            // houseList 출력 테스트
            log.info("----- houseList 출력 테스트 Start -----");
            for(House house : houseList){
                log.info("------------------------------------------");
                log.info("주택유형 : " + house.getHouseType());
                log.info("주택명 : " + house.getHouseName());
                log.info("상세주소 : " + house.getDetailAdr());
                log.info("사용자ID : " + house.getUserId());
                log.info("------------------------------------------");
            }
            log.info("----- houseList 출력 테스트 End -----");

            // 청약홈(하이픈)에서 가져와 house 테이블에 세팅한 해당 사용자의 주택 정보를 모두 삭제
            houseRepository.deleteByUserIdAndSourceType(findUser.getId(), ONE);

            // house 테이블에 houseList 저장
            houseRepository.saveAllAndFlush(houseList);
        }else{
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, "하이픈 보유주택조회 중 오류가 발생했습니다.");
        }

        List<House> houseListFromDB = houseRepository.findByUserId(findUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        List<HouseSimpleInfoResponse> houseSimpleInfoResponseList = new ArrayList<>();

        for(House house : houseListFromDB){
            houseSimpleInfoResponseList.add(
                    HouseSimpleInfoResponse.builder()
                            .houseId(house.getHouseId())
                            .houseType(house.getHouseType())
                            .houseName(house.getHouseName())
                            .roadAddr(house.getRoadAddr())
                            .detailAdr(house.getDetailAdr())
                            .isRequiredDataMissing(checkOwnHouseRequiredDataMissing(house, houseListSearchRequest.getCalcType()))
                            .build());
        }

        return ApiResponse.success(
                HouseListSearchResponse.builder()
                        .listCnt(houseSimpleInfoResponseList.size())
                        .list(houseSimpleInfoResponseList)
                        .build());
    }

    // 보유주택 조회(하이픈-청약홈) 테스트(하이픈 조회 데이터만 DUMMY)
    public Object getHouseListSearchTest(HouseListSearchRequest houseListSearchRequest) {
        log.info(">> [Service]HouseService getHouseListSearchTest - 보유주택 조회(하이픈-청약홈) 테스트");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        // 청약홈(하이픈)에서 가져와 house 테이블에 세팅한 해당 사용자의 주택 정보를 모두 삭제
        houseRepository.deleteByUserIdAndSourceType(findUser.getId(), ONE);

        List<House> houseList = new ArrayList<>();

        // 대경빌라
        houseList.add(
                House.builder()
                        .userId(findUser.getId())
                        .houseType(SIX)
                        .houseName("대경빌라")
                        .detailAdr("1동 8102호")
                        .contractDate(LocalDate.parse("20220423", DateTimeFormatter.ofPattern("yyyyMMdd")))
                        .balanceDate(LocalDate.parse("20220519", DateTimeFormatter.ofPattern("yyyyMMdd")))
                        .buyDate(LocalDate.parse("20220519", DateTimeFormatter.ofPattern("yyyyMMdd")))
                        .buyPrice(Long.parseLong("270000000"))
                        .jibunAddr("경기도 의왕시 삼동 192-2 대경빌라")
                        .roadAddr("경기도 의왕시 부곡중앙북5길 5")
                        .roadAddrRef(" (삼동, 대경빌라)")
                        .bdMgtSn("4143010300101920002009839")
                        .admCd("4143010300")
                        .rnMgtSn("414304403087")
                        .area(new BigDecimal(StringUtils.defaultString("60.840", DEFAULT_DECIMAL)))
                        .isDestruction(false)
                        .isCurOwn(true)
                        .ownerCnt(1)
                        .userProportion(100)
                        .isMoveInRight(false)
                        .sourceType(ONE)
                        .build());

        // 의정부역센트럴자이&위브캐슬
        houseList.add(
                House.builder()
                        .userId(findUser.getId())
                        .houseType(SIX)
                        .houseName("의정부역센트럴자이&위브캐슬")
                        .jibunAddr("경기도 의정부시 의정부동 723 의정부역센트럴자이&위브캐슬")
                        .roadAddr("경기도 의정부시 경의로 130")
                        .roadAddrRef(" (의정부동, 의정부역센트럴자이&위브캐슬)")
                        .bdMgtSn("4115010100103800000015724")
                        .admCd("4115010100")
                        .rnMgtSn("411503181004")
                        .area(new BigDecimal(StringUtils.defaultString("59.948", DEFAULT_DECIMAL)))
                        .isDestruction(false)
                        .isCurOwn(true)
                        .ownerCnt(1)
                        .userProportion(100)
                        .isMoveInRight(false)
                        .sourceType(ONE)
                        .build());

        // house 테이블에 houseList 저장
        houseRepository.saveAllAndFlush(houseList);

        List<House> houseListFromDB = houseRepository.findByUserId(findUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        List<HouseSimpleInfoResponse> houseSimpleInfoResponseList = new ArrayList<>();

        for(House house : houseListFromDB){
            houseSimpleInfoResponseList.add(
                    HouseSimpleInfoResponse.builder()
                            .houseId(house.getHouseId())
                            .houseType(house.getHouseType())
                            .houseName(house.getHouseName())
                            .roadAddr(house.getRoadAddr())
                            .detailAdr(house.getDetailAdr())
                            .isRequiredDataMissing(checkOwnHouseRequiredDataMissing(house, houseListSearchRequest.getCalcType()))
                            .build());
        }

        return ApiResponse.success(
                HouseListSearchResponse.builder()
                        .listCnt(houseSimpleInfoResponseList.size())
                        .list(houseSimpleInfoResponseList)
                        .build());
    }

    // 보유주택 목록 조회(DB)
    public Object getHouseList(String calcType) {
        log.info(">> [Service]HouseService getHouseList - 보유주택 목록 조회(DB)");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        List<House> houseListFromDB = houseRepository.findByUserId(findUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        List<HouseSimpleInfoResponse> houseSimpleInfoResponseList = new ArrayList<>();

        for(House house : houseListFromDB){
            houseSimpleInfoResponseList.add(
                    HouseSimpleInfoResponse.builder()
                            .houseId(house.getHouseId())
                            .houseType(house.getHouseType())
                            .houseName(house.getHouseName())
                            .roadAddr(house.getRoadAddr())
                            .detailAdr(house.getDetailAdr())
                            .isRequiredDataMissing(checkOwnHouseRequiredDataMissing(house, calcType))
                            .build());
        }

        return ApiResponse.success(
                HouseListSearchResponse.builder()
                        .listCnt(houseSimpleInfoResponseList.size())
                        .list(houseSimpleInfoResponseList)
                        .build());
    }

    // 주택 상세정보 조회
    public Object getHouseDetail(Long houseId) throws Exception {
        log.info(">> [Service]HouseService getHouseDetail - 주택 상세정보 조회");

        if(houseId == null || houseId == 0) throw new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR, "주택ID 값이 입력되지 않았습니다.");

        House house = houseRepository.findByHouseId(houseId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        return ApiResponse.success(
                HouseDetailResponse.builder()
                        .houseId(house.getHouseId())
                        .houseType(house.getHouseType())
                        .houseName(house.getHouseName())
                        .detailAdr(house.getDetailAdr())
                        .contractDate(house.getContractDate())
                        .balanceDate(house.getBalanceDate())
                        .buyDate(house.getBuyDate())
                        .buyPrice(house.getBuyPrice())
                        .moveInDate(house.getMoveInDate())
                        .moveOutDate(house.getMoveOutDate())
                        .pubLandPrice(house.getPubLandPrice())
                        .kbMktPrice(house.getKbMktPrice())
                        .jibunAddr(house.getJibunAddr())
                        .roadAddr(house.getRoadAddr())
                        .roadAddrRef(house.getRoadAddrRef())
                        .bdMgtSn(house.getBdMgtSn())
                        .area(house.getArea())
                        .isDestruction(house.getIsDestruction())
                        .ownerCnt(house.getOwnerCnt())
                        .userProportion(house.getUserProportion())
                        .isMoveInRight(house.getIsMoveInRight())
                        .build());
    }

    // 보유주택 (직접)등록
    public Object registHouseInfo(HouseRegistRequest houseRegistRequest) throws Exception {
        log.info(">> [Service]HouseService registHouseInfo - 보유주택 (직접)등록");

        if(houseRegistRequest == null){
            throw new CustomException(ErrorCode.HOUSE_REGIST_ERROR, "등록 주택 정보가 입력되지 않았습니다.");
        }

        Long userId = userUtil.findCurrentUser().getId();   // 호출 사용자

        try{
            houseRepository.saveAndFlush(
                    House.builder()
                            .userId(userId)
                            .houseType(houseRegistRequest.getHouseType())
                            .houseName(houseRegistRequest.getHouseName())
                            .detailAdr(houseRegistRequest.getDetailAdr())
                            .jibunAddr(houseRegistRequest.getJibunAddr())
                            .roadAddr(houseRegistRequest.getRoadAddr())
                            .roadAddrRef(houseRegistRequest.getRoadAddrRef())
                            .bdMgtSn(houseRegistRequest.getBdMgtSn())
                            .admCd(houseRegistRequest.getAdmCd())
                            .rnMgtSn(houseRegistRequest.getRnMgtSn())
                            .isMoveInRight(houseRegistRequest.getIsMoveInRight())
                            .sourceType(TWO)
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.HOUSE_REGIST_ERROR);
        }

        return ApiResponse.success(Map.of("result", "주택 정보가 등록되었습니다."));
    }

    // 보유주택 (정보)수정
    public Object modifyHouseInfo(HouseModifyRequest houseModifyRequest) throws Exception {
        log.info(">> HouseService modifyHouseInfo - 보유주택 (정보)수정");

        if(houseModifyRequest == null){
            log.error("수정 대상 주택 정보가 입력되지 않았습니다.");
            throw new CustomException(ErrorCode.HOUSE_MODIFY_ERROR, "수정 대상 주택 정보가 입력되지 않았습니다.");
        }

        Long houseId = houseModifyRequest.getHouseId();                         // 수정 대상 주택 ID
        Long userId = userUtil.findCurrentUser().getId();                       // 호출 사용자 ID
        Long houseOwnUserId = houseUtil.findSelectedHouse(houseId).getUserId(); // 주택 소유자 ID

        if(houseId == null){
            log.error("수정 대상 주택 ID가 입력되지 않았습니다.");
            throw new CustomException(ErrorCode.HOUSE_MODIFY_ERROR, "수정 대상 주택 ID가 입력되지 않았습니다.");
        }
        if(!userId.equals(houseOwnUserId)){
            log.error("주택 소유자 ID와 사용자 ID가 일치하지 않아 보유주택 정보를 수정할 수 없습니다.");
            throw new CustomException(ErrorCode.HOUSE_MODIFY_ERROR, "주택 소유자 ID와 사용자 ID가 일치하지 않아 보유주택 정보를 수정할 수 없습니다.");
        }

        try{
            houseRepository.saveAndFlush(
                    House.builder()
                            .houseId(houseId)
                            .userId(userId)
                            .houseType(houseModifyRequest.getHouseType())
                            .houseName(houseModifyRequest.getHouseName())
                            .detailAdr(houseModifyRequest.getDetailAdr())
                            .contractDate(houseModifyRequest.getContractDate())
                            .balanceDate(houseModifyRequest.getBalanceDate())
                            .buyDate(houseModifyRequest.getBuyDate())
                            .moveInDate(houseModifyRequest.getMoveInDate())
                            .moveOutDate(houseModifyRequest.getMoveOutDate())
                            .buyPrice(houseModifyRequest.getBuyPrice())
                            .pubLandPrice(houseModifyRequest.getPubLandPrice())
                            .kbMktPrice(houseModifyRequest.getKbMktPrice())
                            .jibunAddr(houseModifyRequest.getJibunAddr())
                            .roadAddr(houseModifyRequest.getRoadAddr())
                            .roadAddrRef(houseModifyRequest.getRoadAddrRef())
                            .bdMgtSn(houseModifyRequest.getBdMgtSn())
                            .admCd(houseModifyRequest.getAdmCd())
                            .rnMgtSn(houseModifyRequest.getRnMgtSn())
                            .area(houseModifyRequest.getArea())
                            .isDestruction(houseModifyRequest.getIsDestruction())
                            .ownerCnt(houseModifyRequest.getOwnerCnt())
                            .userProportion(houseModifyRequest.getUserProportion())
                            .isMoveInRight(houseModifyRequest.getIsMoveInRight())
                            .build());
        }catch(Exception e){
            log.error("주택 테이블 update 중 오류가 발생했습니다.");
            throw new CustomException(ErrorCode.HOUSE_MODIFY_ERROR);
        }

        return ApiResponse.success(Map.of("result", "주택 정보가 수정되었습니다."));
    }

    // 보유주택 삭제
    public Object deleteHouse(HouseListDeleteRequest houseListDeleteRequest) throws Exception {
        log.info(">> [Service]HouseService deleteHouse - 보유주택 삭제");

        Long houseId = houseListDeleteRequest.getHouseId();                     // 삭제 대상 주택 ID
        Long userId = userUtil.findCurrentUser().getId();                       // 호출 사용자 ID
        Long houseOwnUserId = houseUtil.findSelectedHouse(houseId).getUserId(); // 주택 소유자 ID

        if(houseId == null) throw new CustomException(ErrorCode.HOUSE_DELETE_ERROR, "삭제 대상 주택 ID가 입력되지 않았습니다.");
        if(!userId.equals(houseOwnUserId)) throw new CustomException(ErrorCode.HOUSE_DELETE_ERROR, "주택 소유자 ID와 사용자 ID가 일치하지 않아 보유주택 정보를 삭제할 수 없습니다.");

        try{
            houseRepository.deleteByHouseId(houseListDeleteRequest.getHouseId());
        }catch (Exception e){
            throw new CustomException(ErrorCode.HOUSE_DELETE_ERROR);
        }

        return ApiResponse.success(Map.of("result", "보유주택이 삭제되었습니다."));
    }

    // 보유주택 전체 삭제
    public Object deleteHouseAll() throws Exception {
        log.info(">> [Service]HouseService deleteHouse - 보유주택 전체 삭제");

        try{
            houseRepository.deleteByUserId(userUtil.findCurrentUser().getId());
        }catch(Exception e){
            throw new CustomException(ErrorCode.HOUSE_DELETE_ALL_ERROR);
        }

        return ApiResponse.success(Map.of("result", "전체 보유주택이 삭제되었습니다."));
    }

    // (양도주택)거주기간 조회
    public Object getHouseStayPeriod(HouseStayPeriodRequest houseStayPeriodRequest) throws Exception {
        log.info(">> [Service]HouseService getHouseStayPeriod - (양도주택)거주기간 조회");

        HyphenUserResidentRegistrationResponse hyphenUserResidentRegistrationResponse
                = hyphenService.getUserStayPeriodInfo(houseStayPeriodRequest)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR));

        HyphenUserResidentRegistrationData hyphenUserResidentRegistrationData
                = hyphenUserResidentRegistrationResponse.getHyphenUserResidentRegistrationData();

        Long houseId = houseStayPeriodRequest.getHouseId();

        if(houseId == null || houseId == 0) throw new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR, "주택ID 값이 입력되지 않았습니다.");

        House house = houseRepository.findByHouseId(houseId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        HouseStayPeriodResponse houseStayPeriodResponse = null;

        String step = houseStayPeriodRequest.getStep();

        // 로그인 단계 : 1(init)
        if(INIT.equals(step)){
            houseStayPeriodResponse = HouseStayPeriodResponse.builder()
                    .houseId(houseId)
                    .step(step)
                    .stepData(hyphenUserResidentRegistrationData.getStepData())
                    .build();
        }
        // 로그인 단계 : 2(sign)
        else if(SIGN.equals(step)){
            // 양도주택과 동일한 주택을 찾아 거주기간을 계산
            // STEP1 : 변동사유가 '전입'인 항목만 체크
            // STEP2 : 주소가 양도주택과 동일한 경우(주소 분할하여 도로명주소와 지번주소 중 동일한 값이 있는지 확인하고, 상세주소도 비교)
            // STEP3 : 해당 주택의 '신고일'과 다음 건의 '신고일'의 날짜 차이를 계산
            // STEP4 : 날짜 차이를 응답 값의 '거주기간일자'에 세팅
            // STEP5 : 거주기간일자를 x년 y개월로 변경하여 응답값의 '거주기간정보'에 세팅
            // STEP6 : 해당 주택에 언제부터 언제까지 거주했는지를 정리하여 응답값의 '거주기간상세내용'에 세팅

            boolean hasStayInfo = false;                // 거주정보존재여부
            String stayPeriodInfo = EMPTY;              // 거주기간정보
            String stayPeriodCount = EMPTY;             // 거주기간일자
            String stayPeriodDetailContent = EMPTY;     // 거주기간상세내용

            List<ChangeHistory> list = hyphenUserResidentRegistrationData.getChangeHistoryList();

            if(list != null){
                int index = 0;
                for(ChangeHistory history : list){
                    if(hasStayInfo) break;  // 거주정보존재여부가 true이면 반복문 종료

                    // STEP1 : 변동사유가 '전입'인 항목만 체크
                    if(MOVE_IN_KEYWORD.equals(history.getChangeReason())){
                        String address = StringUtils.defaultString(history.getAddress());

                        if(!EMPTY.equals(address)){
                            HouseAddressDto historyAddressDto = houseAddressService.separateAddress(address);
                            HouseAddressDto sellHouseAddressDto = null;

                            if(ONE.equals(historyAddressDto.getAddressType())){
                                sellHouseAddressDto = houseAddressService.separateAddress(house.getJibunAddr());
                            }else{
                                sellHouseAddressDto = houseAddressService.separateAddress(house.getRoadAddr());
                            }

                            if(historyAddressDto != null && sellHouseAddressDto != null){
                                // STEP2 : 주소가 양도주택과 동일한 경우(주소 분할하여 도로명주소와 지번주소 중 동일한 값이 있는지 확인하고, 상세주소도 비교)
                                if(houseAddressService.compareAddress(historyAddressDto, sellHouseAddressDto)){
                                    hasStayInfo = true; // 거주정보 존재함으로 세팅

                                    String curReportDate = history.getReportDate();
                                    String nextReportDate = EMPTY;

                                    for(int i=index+1; i<list.size(); i++){
                                        if(MOVE_IN_KEYWORD.equals(history.getChangeReason())){
                                            nextReportDate = StringUtils.defaultString(list.get(i).getReportDate());
                                            break;
                                        }
                                    }

                                    // STEP3 : 해당 주택의 '신고일'과 다음 건의 '신고일'의 날짜 차이를 계산
                                    if(!EMPTY.equals(curReportDate)){
                                        LocalDate crDate = LocalDate.parse(curReportDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                                        LocalDate nrDate = null;

                                        // 다음 전입 신고일이 존재하는 경우
                                        if(!EMPTY.equals(nextReportDate)){
                                            nrDate = LocalDate.parse(nextReportDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                                        }
                                        // 다음 전입 신고일이 존재하지 않는 경우(해당 주택에서 현재까지 계속 거주하고 있는 경우)
                                        else{
                                            nrDate = LocalDate.now();
                                        }

                                        Long stayPeriodByDay = ChronoUnit.DAYS.between(crDate, nrDate);
                                        Long stayPeriodByMonth = ChronoUnit.MONTHS.between(crDate, nrDate);
                                        Long stayPeriodByYear = ChronoUnit.YEARS.between(crDate, nrDate);

                                        // 거주기간이 하루 이상은 되어야 세팅
                                        if(stayPeriodByDay > 0){
                                            // STEP4 : 날짜 차이를 n일로 변경하여 응답값의 '거주기간일자'에 세팅
                                            stayPeriodCount = Long.toString(stayPeriodByDay) + "일";

                                            // STEP5 : 거주기간일자를 x년 y개월로 변경하여 응답값의 '거주기간정보'에 세팅
                                            if(stayPeriodByYear > 0){
                                                stayPeriodInfo = Long.toString(stayPeriodByYear) + "년 " + (stayPeriodByMonth - (stayPeriodByYear * 12)) + "개월";
                                            }else{
                                                stayPeriodInfo = Long.toString(stayPeriodByMonth) + "개월";
                                            }

                                            // STEP6 : 해당 주택에 언제부터 언제까지 거주했는지를 정리하여 응답값의 '거주기간상세내용'에 세팅
                                            stayPeriodDetailContent = curReportDate + "부터 " + nextReportDate + "까지 거주";
                                        }
                                    }
                                }
                            }
                        }
                    }

                    index++;
                }
            }else{
                throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR);
            }

            houseStayPeriodResponse = HouseStayPeriodResponse.builder()
                    .houseId(houseId)
                    .step(step)
                    .stepData(hyphenUserResidentRegistrationData.getStepData())
                    .houseName(house.getHouseName())
                    .detailAdr(house.getDetailAdr())
                    .hasStayInfo(hasStayInfo)
                    .stayPeriodInfo(stayPeriodInfo)
                    .stayPeriodCount(stayPeriodCount)
                    .stayPeriodDetailContent(stayPeriodDetailContent)
                    .build();
        }

        return ApiResponse.success(houseStayPeriodResponse);
    }

    // 하이픈 보유주택 조회 응답 정상여부 확인
    private boolean isSuccessHyphenUserHouseListResponse(HyphenCommon hyphenCommon){
        log.info(">>> HouseService isSuccessHyphenUserHouseListResponse - 하이픈 보유주택 조회 응답 정상여부 확인");

        if(hyphenCommon != null){
            if(NO.equals(hyphenCommon.getErrYn())){
                return true;
            }else{
                log.error("하이픈 오류 발생 : " + hyphenCommon.getErrMsg());
                return false;
            }
        }else{
            return false;
        }
    }

    // 하이픈에서 가져온 List1(건축물대장정보)을 House 엔티티에 필터링하여 세팅
    private void setList1ToHouseEntity(List<DataDetail1> list, List<House> houseList){
        log.info(">>> HouseService setList1ToHouseEntity - 하이픈에서 가져온 List1(건축물대장정보)을 House 엔티티에 필터링하여 세팅");

        List<HyphenUserHouseResultInfo> hyphenUserHouseResultInfoList = new ArrayList<>();

        if(list != null && !list.isEmpty()){
            for (DataDetail1 dataDetail1 : list) {
                HouseAddressDto houseAddressDto = houseAddressService.separateAddress(dataDetail1.getAddress());

                String publishedPrice = StringUtils.defaultString(dataDetail1.getPublishedPrice(), ZERO);
                if(!publishedPrice.matches("^[0-9]+$")){
                    publishedPrice = ZERO;
                }

                hyphenUserHouseResultInfoList.add(
                        HyphenUserHouseResultInfo.builder()
                                .resultListNo(ONE)
                                .tradeType(ZERO)
                                .orgAdr(houseAddressDto.getAddress())
                                .searchAdr(houseAddressDto.getSearchAddress())
                                .houseType(SIX)
                                .pubLandPrice(Long.parseLong(publishedPrice))
                                .area(new BigDecimal(StringUtils.defaultString(dataDetail1.getArea(), DEFAULT_DECIMAL)))
                                .build());
            }
        }

        if(!hyphenUserHouseResultInfoList.isEmpty()){
            for (HyphenUserHouseResultInfo hyphenUserHouseResultInfo : hyphenUserHouseResultInfoList){
                JusoDetail jusoDetail = this.searchJusoDetail(hyphenUserHouseResultInfo);
                this.setHouseList(hyphenUserHouseResultInfo, jusoDetail, houseList); // House Entity에 데이터 세팅
            }
        }
    }

    // 하이픈에서 가져온 List2(부동산거래내역)을 House 엔티티에 필터링하여 세팅
    private void setList2ToHouseEntity(List<DataDetail2> list, List<House> houseList){
        log.info(">>> HouseService setList2ToHouseEntity - 하이픈에서 가져온 List2(부동산거래내역)을 House 엔티티에 필터링하여 세팅");

        List<HyphenUserHouseResultInfo> tempHyphenUserHouseResultInfoList = new ArrayList<>();

        if(list != null && !list.isEmpty()){
            for (DataDetail2 dataDetail2 : list) {
                HouseAddressDto houseAddressDto = houseAddressService.separateAddress(dataDetail2.getAddress());

                String tradeType = this.getTradeTypeFromSellBuyClassification(StringUtils.defaultString(dataDetail2.getSellBuyClassification()));
                String houseType = this.getHouseTypeFromSellBuyClassification(StringUtils.defaultString(dataDetail2.getSellBuyClassification()));

                String buyPrice = ZERO;
                String sellPrice = ZERO;

                // 매수
                if(ONE.equals(tradeType)){
                    buyPrice = StringUtils.defaultString(dataDetail2.getTradingPrice(), ZERO);
                }
                // 매도
                else if(TWO.equals(tradeType)){
                    sellPrice = StringUtils.defaultString(dataDetail2.getTradingPrice(), ZERO);
                }

                tempHyphenUserHouseResultInfoList.add(
                        HyphenUserHouseResultInfo.builder()
                                .resultListNo(TWO)
                                .tradeType(tradeType)
                                .orgAdr(houseAddressDto.getAddress())
                                .searchAdr(houseAddressDto.getSearchAddress())
                                .houseType(houseType)
                                .contractDate(LocalDate.parse(dataDetail2.getContractDate(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .balanceDate(LocalDate.parse(dataDetail2.getBalancePaymentDate(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .buyPrice(Long.parseLong(buyPrice))
                                .sellPrice(Long.parseLong(sellPrice))
                                .area(new BigDecimal(StringUtils.defaultString(dataDetail2.getArea(), DEFAULT_DECIMAL)))
                                .build());
            }
            
            // 거래내역 주택 필터링 작업하여 hyphenUserHouseResultInfoList에 결과 세팅
            List<HyphenUserHouseResultInfo> hyphenUserHouseResultInfoList = this.filteringTradeHouseList(tempHyphenUserHouseResultInfoList);

            // 도로명주소 검색 API 호출(주소기반산업지원서비스)
            if(!hyphenUserHouseResultInfoList.isEmpty()){
                for (HyphenUserHouseResultInfo hyphenUserHouseResultInfo : hyphenUserHouseResultInfoList){
                    JusoDetail jusoDetail = this.searchJusoDetail(hyphenUserHouseResultInfo);
                    this.setHouseList(hyphenUserHouseResultInfo, jusoDetail, houseList); // House Entity에 데이터 세팅
                }
            }
        }
    }

    // 하이픈에서 가져온 List3(재산세정보)을 House 엔티티에 필터링하여 세팅
    private void setList3ToHouseEntity(List<DataDetail3> list, List<House> houseList){
        log.info(">>> HouseService setList3ToHouseEntity - 하이픈에서 가져온 List3(재산세정보)을 House 엔티티에 필터링하여 세팅");

        List<HyphenUserHouseResultInfo> hyphenUserHouseResultInfoList = new ArrayList<>();

        if(list != null && !list.isEmpty()){
            for (DataDetail3 dataDetail3 : list) {
                HouseAddressDto houseAddressDto = houseAddressService.separateAddress(dataDetail3.getAddress());

                hyphenUserHouseResultInfoList.add(
                        HyphenUserHouseResultInfo.builder()
                                .resultListNo(THREE)
                                .tradeType(ZERO)
                                .orgAdr(houseAddressDto.getAddress())
                                .searchAdr(houseAddressDto.getSearchAddress())
                                .detailAdr(houseAddressDto.getDetailAddress())
                                .houseType(SIX)
                                .buyDate(LocalDate.parse(dataDetail3.getAcquisitionDate(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .area(new BigDecimal(StringUtils.defaultString(dataDetail3.getArea(), DEFAULT_DECIMAL)))
                                .build());
            }
        }

        if(!hyphenUserHouseResultInfoList.isEmpty()){
            for (HyphenUserHouseResultInfo hyphenUserHouseResultInfo : hyphenUserHouseResultInfoList){
                JusoDetail jusoDetail = this.searchJusoDetail(hyphenUserHouseResultInfo);
                this.setHouseList(hyphenUserHouseResultInfo, jusoDetail, houseList);
            }
        }
    }

    // 거래내역 주택 필터링
    private List<HyphenUserHouseResultInfo> filteringTradeHouseList(List<HyphenUserHouseResultInfo> list){
        log.info(">>> HouseService filteringTradeHouseList - 거래내역 주택 필터링");

        List<HyphenUserHouseResultInfo> filterList = new ArrayList<>();
        List<HyphenUserHouseResultInfo> doubleBuyList = new ArrayList<>();
        List<HyphenUserHouseResultInfo> resultList = new ArrayList<>();
        List<String> removeAddressList = new ArrayList<>();

        if(list != null){
            for(HyphenUserHouseResultInfo hyphenUserHouseResultInfo : list){
                filterList.add(hyphenUserHouseResultInfo.clone());
            }
        }

        // Step 1. 동일한 '주소'의 '매수' 건이 2개 이상 존재하면 doubleBuyList에 모두 추가하고, filterList에서 모두 삭제
        if(!filterList.isEmpty()){
            log.info("Step 1. 동일한 '주소'의 '매수' 건이 2개 이상 존재하면 doubleBuyList에 모두 추가하고, filterList에서 모두 삭제");
            for(int i=0; i<filterList.size(); i++){
                // 거래유형이 '매수'인 건을 비교 대상으로 세팅
                if(ONE.equals(filterList.get(i).getTradeType())){
                    String compareAddress = StringUtils.defaultString(filterList.get(i).getOrgAdr());
                    boolean isAdd = false;

                    if(!removeAddressList.contains(compareAddress)){
                        for(int j=0; j<filterList.size(); j++){
                            // 거래유형이 '매수'인 건과 비교(같은 index는 제외)
                            if(j!=i && ONE.equals(filterList.get(j).getTradeType())){
                                // doubleBuyList에 추가
                                if(compareAddress.equals(filterList.get(j).getOrgAdr())){
                                    if(!isAdd){
                                        doubleBuyList.add(filterList.get(i).clone());
                                        removeAddressList.add(compareAddress);  // removeAddressList에 주소 추가
                                        isAdd = true;
                                    }
                                    doubleBuyList.add(filterList.get(j).clone());
                                }
                            }
                        }
                    }
                }
            }

            for(String compareAddress : removeAddressList){
                filterList.removeIf(filterInfo -> ONE.equals(filterInfo.getTradeType()) && compareAddress.equals(filterInfo.getOrgAdr()));
            }

            removeAddressList.clear();  // removeList 초기화
        }

        // Step 2. 동일한 '주소'의 '매수', '매도' 건이 쌍으로 존재하면 filterList에서 둘 다 삭제
        if(!filterList.isEmpty()){
            log.info("Step 2. 동일한 '주소'의 '매수', '매도' 건이 쌍으로 존재하면 filterList에서 둘 다 삭제");
            for(int i=0; i<filterList.size(); i++){
                // 거래유형이 '매수'인 건을 비교 대상으로 세팅
                if(ONE.equals(filterList.get(i).getTradeType())){
                    String compareAddress = StringUtils.defaultString(filterList.get(i).getOrgAdr());

                    for(int j=0; j<filterList.size(); j++){
                        // 거래유형이 '매도'인 건과 비교
                        if(TWO.equals(filterList.get(j).getTradeType())){
                            if(!removeAddressList.contains(filterList.get(j).getOrgAdr())){
                                if(compareAddress.equals(filterList.get(j).getOrgAdr())){
                                    removeAddressList.add(compareAddress);  // removeAddressList에 주소 추가
                                }
                            }
                        }
                    }
                }
            }

            for(String compareAddress : removeAddressList){
                filterList.removeIf(filterInfo -> compareAddress.equals(filterInfo.getOrgAdr()));
            }

            removeAddressList.clear();  // removeList 초기화
        }

        // Step 3. filterList에 남은 '매도' 건 중 doubleBuyList와 비교하여 동일한 '주소'가 존재하면 filterList에서 삭제
        if(!filterList.isEmpty()){
            log.info("Step 3. filterList에 남은 '매도' 건 중 doubleBuyList와 비교하여 동일한 '주소'가 존재하면 filterList에서 삭제");
            for(HyphenUserHouseResultInfo filterInfo : filterList){
                if(TWO.equals(filterInfo.getTradeType())){
                    String compareAddress = StringUtils.defaultString(filterInfo.getOrgAdr());

                    for(HyphenUserHouseResultInfo resultInfo : doubleBuyList){
                        if(compareAddress.equals(resultInfo.getOrgAdr())){
                            removeAddressList.add(compareAddress);
                        }
                    }
                }
            }

            for(String compareAddress : removeAddressList){
                filterList.removeIf(filterInfo -> TWO.equals(filterInfo.getTradeType()) && compareAddress.equals(filterInfo.getOrgAdr()));
            }

            removeAddressList.clear();  // removeList 초기화
        }

        // Step 4. filterList에 남은 '매도' 건의 '검색주소'와 '매수' 건의 '검색주소'를 비교하여 동일한 주소가 존재하면 함께 삭제
        if(!filterList.isEmpty()){
            log.info("Step 4. filterList에 남은 '매도' 건의 '검색주소'와 '매수' 건의 '검색주소'를 비교하여 동일한 주소가 존재하면 함께 삭제");
            for(int i=0; i<filterList.size(); i++){
                // 거래유형이 '매도'인 건을 비교 대상으로 세팅
                if(TWO.equals(filterList.get(i).getTradeType())){
                    //String compareAddress = StringUtils.defaultString(filterList.get(i).getOrgAdr());
                    String compareSearchAddress = EMPTY;
                    if(filterList.get(i).getSearchAdr() != null){
                        compareSearchAddress = StringUtils.defaultString(filterList.get(i).getSearchAdr().get(0));
                    }

                    for(int j=0; j<filterList.size(); j++){
                        // 거래유형이 '매수'인 건과 비교
                        if(ONE.equals(filterList.get(j).getTradeType())){
                            if(!removeAddressList.contains(filterList.get(j).getSearchAdr().get(0))){
                                if(compareSearchAddress.equals(filterList.get(j).getSearchAdr().get(0))){
                                    removeAddressList.add(compareSearchAddress);  // removeAddressList에 주소 추가
                                }
                            }
                        }
                    }
                }
            }

            for(String compareSearchAddress : removeAddressList){
                filterList.removeIf(filterInfo -> compareSearchAddress.equals(filterInfo.getSearchAdr().get(0)));
            }

            removeAddressList.clear();  // removeList 초기화
        }

        // Step 5. filterList에 남은 '매수' 건은 모두 resultList에 추가하고 filterList에서는 삭제
        if(!filterList.isEmpty()){
            log.info("Step 5. filterList에 남은 '매수' 건은 모두 resultList에 추가하고 filterList에서는 삭제");
            for(HyphenUserHouseResultInfo filterInfo : filterList){
                if(ONE.equals(filterInfo.getHouseType())){
                    resultList.add(filterInfo.clone());
                }
            }

            filterList.removeIf(filterInfo -> ONE.equals(filterInfo.getTradeType()));
        }

        if(!filterList.isEmpty()){
            // filterList에 남은 항목 로그 출력
            log.info("--- FilterList print All Start ---");
            for(HyphenUserHouseResultInfo filterInfo : filterList){
                log.info("거래구분 : " + filterInfo.getTradeType());
                log.info("원장주소 : " + filterInfo.getOrgAdr());
            }
            log.info("--- FilterList print All End ---");
        }

        // Step 6. doubleBuyList의 항목을 모두 resultList에 추가
        log.info("Step 6. doubleBuyList의 항목을 모두 resultList에 추가");
        resultList.addAll(doubleBuyList);

        return resultList;
    }

    // (매도/매수구분에서) 거래유형 가져오기
    private String getTradeTypeFromSellBuyClassification(String sellBuyClassification){
        log.info(">>> HouseService getTradeTypeFromSellBuyClassification - (매도/매수구분에서) 거래유형 가져오기");

        String tradeType = ZERO;

        if(sellBuyClassification.contains("매수")){
            tradeType = ONE;
        }else if(sellBuyClassification.contains("매도")){
            tradeType = TWO;
        }

        return tradeType;
    }

    // (매도/매수구분에서) 주택유형 가져오기
    private String getHouseTypeFromSellBuyClassification(String sellBuyClassification){
        log.info(">>> HouseService getHouseTypeFromSellBuyClassification - (매도/매수구분에서) 주택유형 가져오기");

        String houseType = SIX;

        if(sellBuyClassification.contains("분양권")){
            houseType = FIVE;
        }

        return houseType;
    }

    // (주소기반산업지원서비스 - 도로명주소 조회) 상세 주소정보 조회
    private JusoDetail searchJusoDetail(HyphenUserHouseResultInfo hyphenUserHouseResultInfo){
        log.info(">>> HouseService searchJusoDetail - (주소기반산업지원서비스 - 도로명주소 조회) 상세 주소정보 조회");

        JusoDetail jusoDetail = null;

        if(hyphenUserHouseResultInfo.getSearchAdr() != null){
            // 주소기반산업지원서비스 도로명주소 검색은 한 건당 최대 (3)회 까지로 제한
            for(int i=0; i<MAX_JUSO_CALL_CNT; i++){
                if(hyphenUserHouseResultInfo.getSearchAdr().get(i) != null){
                    StringBuilder searchAddr = new StringBuilder(EMPTY);
                    if(!EMPTY.contentEquals(searchAddr)){
                        searchAddr.append(SPACE);
                    }

                    // hyphenUserHouseResultInfo에서 검색주소 추출(조회 결과가 2건 이상인 경우 파라미터 하나씩 추가하여 재조회)
                    searchAddr.append(hyphenUserHouseResultInfo.getSearchAdr().get(i));

                    // 도로명주소 검색 API 호출(주소기반산업지원서비스)
                    JusoGovRoadAdrResponse jusoGovRoadAdrResponse = jusoGovService.getRoadAdrInfo(searchAddr.toString());

                    if(jusoGovRoadAdrResponse != null && jusoGovRoadAdrResponse.getResults() != null && jusoGovRoadAdrResponse.getResults().getCommon() != null){
                        String totalCount = jusoGovRoadAdrResponse.getResults().getCommon().getTotalCount();
                        String errorCode = jusoGovRoadAdrResponse.getResults().getCommon().getErrorCode();
                        String errorMessage = jusoGovRoadAdrResponse.getResults().getCommon().getErrorMessage();

                        // 정상
                        if(ZERO.equals(errorCode)){
                            try{
                                int ttcn = Integer.parseInt(totalCount);

                                // 검색 결과가 1건인 경우 : List<House>에 데이터 세팅
                                if(ttcn == 1){
                                    //this.setHouseList(hyphenUserHouseResultInfo, jusoGovRoadAdrResponse.getResults().getJuso().get(0), houseList);
                                    jusoDetail = jusoGovRoadAdrResponse.getResults().getJuso().get(0);
                                    jusoDetail = houseAddressService.replaceSpecialCharactersForJusoDetail(jusoDetail);
                                    break;
                                }
                                // 검색 결과가 없는 경우 : PASS
                                else if(ttcn == 0){
                                    log.info(searchAddr + "주소의 검색 결과가 없습니다.(주소기반산업지원서비스)");
                                    break;
                                }
                            }catch(NumberFormatException e){
                                // TODO. 오류 처리 로직 추가
                                log.error("검색 결과 Count 형변환(Integer) 실패");
                            }
                        }
                        // 오류
                        else{
                            // TODO. 오류 처리 로직 추가
                            log.error("도로명주소 검색 중 오류 발생\n" + errorCode + " : " + errorMessage);
                        }
                    }
                    else{
                        // TODO. 오류 처리 로직 추가
                        log.error("도로명주소 검색 API 응답 없음");
                    }
                }
            }
        }

        return jusoDetail;
    }

    // House Entity List 세팅
    private void setHouseList(HyphenUserHouseResultInfo hyphenUserHouseResultInfo, JusoDetail jusoDetail, List<House> houseList){
        log.info(">>> HouseService setHouseList - House Entity List 세팅");

        if(hyphenUserHouseResultInfo != null && jusoDetail != null && houseList != null){
            // houseList에 동일한 건물관리번호를 가진 House가 존재하는지 체크하여 존재하면 update, 존재하지 않으면 create
            String bdMgtSn = jusoDetail.getBdMgtSn();
            String resultListNo = hyphenUserHouseResultInfo.getResultListNo();
            boolean isUpdate = false;

            for (House house : houseList){
                // update(동일한 건물관리번호를 가진 House 존재)
                if(bdMgtSn != null && bdMgtSn.equals(house.getBdMgtSn())){
                    // List1 : 건축물대장정보
                    if(ONE.equals(resultListNo)){
                        if(house.getPubLandPrice() == null || house.getPubLandPrice() == 0) house.setPubLandPrice(hyphenUserHouseResultInfo.getPubLandPrice());
                        if(house.getArea() == null) house.setArea(hyphenUserHouseResultInfo.getArea());
                    }
                    // List2 : 부동산거래내역
                    else if(TWO.equals(resultListNo)){
                        if(house.getContractDate() == null) house.setContractDate(hyphenUserHouseResultInfo.getContractDate());
                        if(house.getBalanceDate() == null) house.setBalanceDate(hyphenUserHouseResultInfo.getBalanceDate());
                        if(house.getBuyPrice() == null || house.getBuyPrice() == 0) house.setBuyPrice(hyphenUserHouseResultInfo.getBuyPrice());
                    }
                    // List3 : 재산세정보
                    else if(THREE.equals(resultListNo)){
                        if(house.getDetailAdr().isBlank()) house.setDetailAdr(hyphenUserHouseResultInfo.getDetailAdr());
                        if(house.getBuyDate() == null) house.setBuyDate(hyphenUserHouseResultInfo.getBuyDate());
                        if(house.getArea() == null) house.setArea(hyphenUserHouseResultInfo.getArea());
                    }

                    // 기존 데이터의 주택유형이 '6'(주택)인 경우에는 업데이트
                    if(house.getHouseType().isBlank() || (SIX.equals(house.getHouseType()) && !SIX.equals(hyphenUserHouseResultInfo.getHouseType()))){
                        house.setHouseType(hyphenUserHouseResultInfo.getHouseType());
                    }

                    if(house.getHouseName().isBlank()) house.setHouseName(jusoDetail.getBdNm());
                    if(house.getJibunAddr().isBlank()) house.setJibunAddr(jusoDetail.getJibunAddr());
                    if(house.getRoadAddr().isBlank()) house.setRoadAddr(jusoDetail.getRoadAddrPart1());
                    if(house.getRoadAddrRef().isBlank()) house.setRoadAddrRef(jusoDetail.getRoadAddrPart2());
                    if(house.getBdMgtSn().isBlank()) house.setBdMgtSn(jusoDetail.getBdMgtSn());
                    if(house.getAdmCd().isBlank()) house.setAdmCd(jusoDetail.getAdmCd());
                    if(house.getRnMgtSn().isBlank()) house.setRnMgtSn(jusoDetail.getRnMgtSn());

                    house.setIsDestruction(false);
                    house.setOwnerCnt(1);
                    house.setUserProportion(100);
                    house.setIsMoveInRight(false);
                    house.setSourceType(ONE);

                    isUpdate = true;
                    log.info("House Update : " + jusoDetail.getBdMgtSn() + " : " + jusoDetail.getBdNm());
                    break;
                }
            }

            // create(동일한 건물관리번호를 가진 House가 존재하지 않음)
            if(!isUpdate){
                if(ONE.equals(resultListNo)){
                    houseList.add(
                            House.builder()
                                    .houseType(hyphenUserHouseResultInfo.getHouseType())
                                    .houseName(jusoDetail.getBdNm())
                                    .pubLandPrice(hyphenUserHouseResultInfo.getPubLandPrice())
                                    .jibunAddr(jusoDetail.getJibunAddr())
                                    .roadAddr(jusoDetail.getRoadAddrPart1())
                                    .roadAddrRef(jusoDetail.getRoadAddrPart2())
                                    .bdMgtSn(jusoDetail.getBdMgtSn())
                                    .admCd(jusoDetail.getAdmCd())
                                    .rnMgtSn(jusoDetail.getRnMgtSn())
                                    .area(hyphenUserHouseResultInfo.getArea())
                                    .isDestruction(false)
                                    .ownerCnt(1)
                                    .userProportion(100)
                                    .isMoveInRight(false)
                                    .sourceType(ONE)
                                    .build());
                }else if(TWO.equals(resultListNo)){
                    houseList.add(
                            House.builder()
                                    .houseType(hyphenUserHouseResultInfo.getHouseType())
                                    .houseName(jusoDetail.getBdNm())
                                    .contractDate(hyphenUserHouseResultInfo.getContractDate())
                                    .balanceDate(hyphenUserHouseResultInfo.getBalanceDate())
                                    .buyDate(hyphenUserHouseResultInfo.getBuyDate())
                                    .buyPrice(hyphenUserHouseResultInfo.getBuyPrice())
                                    .jibunAddr(jusoDetail.getJibunAddr())
                                    .roadAddr(jusoDetail.getRoadAddrPart1())
                                    .roadAddrRef(jusoDetail.getRoadAddrPart2())
                                    .bdMgtSn(jusoDetail.getBdMgtSn())
                                    .admCd(jusoDetail.getAdmCd())
                                    .rnMgtSn(jusoDetail.getRnMgtSn())
                                    .area(hyphenUserHouseResultInfo.getArea())
                                    .isDestruction(false)
                                    .ownerCnt(1)
                                    .userProportion(100)
                                    .isMoveInRight(false)
                                    .sourceType(ONE)
                                    .build());
                }else if(THREE.equals(resultListNo)){
                    houseList.add(
                            House.builder()
                                    .houseType(hyphenUserHouseResultInfo.getHouseType())
                                    .houseName(jusoDetail.getBdNm())
                                    .detailAdr(hyphenUserHouseResultInfo.getDetailAdr())
                                    .buyDate(hyphenUserHouseResultInfo.getBuyDate())
                                    .jibunAddr(jusoDetail.getJibunAddr())
                                    .roadAddr(jusoDetail.getRoadAddrPart1())
                                    .roadAddrRef(jusoDetail.getRoadAddrPart2())
                                    .bdMgtSn(jusoDetail.getBdMgtSn())
                                    .admCd(jusoDetail.getAdmCd())
                                    .rnMgtSn(jusoDetail.getRnMgtSn())
                                    .area(hyphenUserHouseResultInfo.getArea())
                                    .isDestruction(false)
                                    .ownerCnt(1)
                                    .userProportion(100)
                                    .isMoveInRight(false)
                                    .sourceType(ONE)
                                    .build());
                }

                log.info("House Create : " + jusoDetail.getBdMgtSn() + " : " + jusoDetail.getBdNm());
            }
        }else{
            // TODO. 오류 처리 로직 추가
            log.error("House 정보 세팅 실패하였습니다.");
        }
    }

    // 보유주택 필수 데이터 누락여부 체크
    private boolean checkOwnHouseRequiredDataMissing(House house, String calcType){
        log.info(">>> HouseService checkOwnHouseRequiredDataMissing - 보유주택 필수 데이터 누락 여부 체크");

        boolean isRequiredDataMissing = false;

        // 주택유형 데이터가 없는 경우 필수 데이터 누락 '여'
        if(house.getHouseType() == null || EMPTY.equals(house.getHouseType())){
            log.info("주택유형 데이터가 없는 경우 필수 데이터 누락");
            isRequiredDataMissing = true;
        }

        // 상세주소 데이터가 없으면 필수데이터 누락 '여'
        if(house.getDetailAdr() == null || EMPTY.equals(house.getDetailAdr())){
            log.info("상세주소 데이터가 없으면 필수데이터 누락");
            isRequiredDataMissing = true;
        }

        // 지번주소 및 도로명주소 데이터가 없으면 필수데이터 누락 '여'
        if(house.getJibunAddr() == null || EMPTY.equals(house.getJibunAddr()) || house.getRoadAddr() == null || EMPTY.equals(house.getRoadAddr())){
            log.info("지번주소 및 도로명주소 데이터가 없으면 필수데이터 누락");
            isRequiredDataMissing = true;
        }

        // 건물관리번호 데이터가 없으면 필수데이터 누락 '여'
        if(house.getBdMgtSn() == null || EMPTY.equals(house.getBdMgtSn())){
            log.info("건물관리번호 데이터가 없으면 필수데이터 누락");
            isRequiredDataMissing = true;
        }

        // 양도소득세 계산의 보유주택조회인 경우
        if(CALC_TYPE_SELL.equals(calcType)){
            // 계약일자, 취득일자 또는 취득가격 데이터가 없으면 필수데이터 누락 '여'
            if(house.getContractDate() == null || house.getBuyDate() == null || house.getBuyPrice() == null){
                log.info("양도소득세 계산의 보유주택조회인 경우 - 계약일자, 취득일자 또는 취득가격 데이터가 없으면 필수데이터 누락");
                isRequiredDataMissing = true;
            }
        }
        log.info("isRequiredDataMissing : " + isRequiredDataMissing);

        return isRequiredDataMissing;
    }
}
