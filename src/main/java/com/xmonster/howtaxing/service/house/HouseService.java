package com.xmonster.howtaxing.service.house;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.house.*;
import com.xmonster.howtaxing.dto.house.HouseListSearchResponse.HouseSimpleInfoResponse;
import com.xmonster.howtaxing.dto.house.HouseSaveAllRequest.HouseSaveRequest;
import com.xmonster.howtaxing.dto.hyphen.HyphenAuthResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseResultInfo;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse.HyphenUserResidentRegistrationData;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse.HyphenUserResidentRegistrationData.ChangeHistory;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserSessionRequest;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAdrResponse;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAdrResponse.Results.JusoDetail;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListResponse.HyphenCommon;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListResponse.HyphenData.*;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.model.LoadHouse;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.service.redis.RedisService;
import com.xmonster.howtaxing.type.ErrorCode;

import com.xmonster.howtaxing.utils.HouseUtil;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import javax.transaction.Transactional;
import javax.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HouseService {

    private final HyphenService hyphenService;
    private final JusoGovService jusoGovService;
    private final HouseAddressService houseAddressService;
    private final RedisService redisService;

    private final HouseRepository houseRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;

    private final ObjectMapper objectMapper;
    private final Environment environment;

    private static final int MAX_JUSO_CALL_CNT = 3; // 주소 한건 당 주소기반산업지원서비스 도로명주소 재조회 호출 건수 최대값

    // 보유주택 조회(하이픈-청약홈)
    public Object getHouseListSearch(HouseListSearchRequest houseListSearchRequest) {
        log.info(">> [Service]HouseService getHouseListSearch - 보유주택 조회(하이픈-청약홈)");

        String[] activeProfiles = environment.getActiveProfiles();
        boolean isLocal = Arrays.asList(activeProfiles).contains("local");
        
        HyphenUserHouseListResponse hyphenUserHouseListResponse;
        if (isLocal) {
            // 로컬환경 : 하이픈 주택소유정보 테스트용 json data 세팅
            hyphenUserHouseListResponse = getMockedHyphenUserHouseListResponse(houseListSearchRequest.getUserNm());
        } else {
            // 그 외 개발/운영 등 : 하이픈 주택소유정보 조회 호출
            hyphenUserHouseListResponse = hyphenService.getUserHouseInfo(houseListSearchRequest)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR));
        }

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

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

            // 각 리스트를 다시 사용하여 필요한 빈 값을 채워넣기
            this.fillEmptyValuesFromLists(list1, list2, list3, houseList);

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

        List<House> houseListFromDB = houseUtil.findOwnHouseList();
        List<HouseSimpleInfoResponse> houseSimpleInfoResponseList = new ArrayList<>();

        if(houseListFromDB != null){
            for(House house : houseListFromDB){
                houseSimpleInfoResponseList.add(
                        HouseSimpleInfoResponse.builder()
                                .houseId(house.getHouseId())
                                .houseType(house.getHouseType())
                                .houseName(house.getHouseName())
                                .roadAddr(house.getRoadAddr())
                                .detailAdr(house.getDetailAdr())
                                .isMoveInRight(house.getIsMoveInRight())
                                .isRequiredDataMissing(checkOwnHouseRequiredDataMissing(house, houseListSearchRequest.getCalcType()))
                                .build());
            }
        }

        return ApiResponse.success(
                HouseListSearchResponse.builder()
                        .listCnt(houseSimpleInfoResponseList.size())
                        .list(houseSimpleInfoResponseList)
                        .build());
    }

    // 부동산거래내역 기반 보유주택 출력
    public Object loadHouseFromRealty(HouseListSearchRequest houseListSearchRequest) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isLocal = Arrays.asList(activeProfiles).contains("local");
        //boolean isDev = Arrays.asList(activeProfiles).contains("dev");

        if(houseListSearchRequest == null){
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_INPUT_ERROR);
        }

        boolean isDummy = (houseListSearchRequest.getIsDummy() != null) ? houseListSearchRequest.getIsDummy() : false;
        
        HyphenUserHouseListResponse hyphenUserHouseListResponse;
        //if(isLocal){
        if (isDummy) {
            // 로컬환경 : 하이픈 주택소유정보 테스트용 json data 세팅
            hyphenUserHouseListResponse = getMockedHyphenUserHouseListResponse(houseListSearchRequest.getUserNm());
        } else {
            // 그 외 개발/운영 등 : 하이픈 주택소유정보 조회 호출
            hyphenUserHouseListResponse = hyphenService.getUserHouseInfo(houseListSearchRequest)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR));
        }

        // 사용자 정보 가져오기
        User findUser = userUtil.findCurrentUser();
        Long userId = findUser.getId();

        // 건축물대장정보 가져오기
        List<DataDetail1> list1 = hyphenUserHouseListResponse.getHyphenData().getList1();
        saveBuildingInfo(userId, list1);    // 건축물대장정보 세션 저장

        // 부동산거래내역 가져오기
        List<DataDetail2> list2 = hyphenUserHouseListResponse.getHyphenData().getList2();

        // 재산세정보 가져오기
        List<DataDetail3> list3 = hyphenUserHouseListResponse.getHyphenData().getList3();
        savePropertyInfo(userId, list3);    // 재산세정보 세션 저장
        
        // 부동산거래내역 중 매수거래만 추출
        List<DataDetail2> buyTransactions = createBuyTransactions(list2);
        saveTradingInfo(userId, buyTransactions);   // 매수거래내역 세션 저장

        List<LoadHouse> houseList = new ArrayList<>();  // 보유주택 목록
        JusoDetail jusoDetail = null;

        /*
         * ##########################################
         * 매수거래내역 주택 주소기반에서 검색 후 주택정보 세팅
         * ##########################################
         */
        for (DataDetail2 dataDetail2 : buyTransactions) {
            HouseAddressDto houseAddressDto = houseAddressService.parseAddress(dataDetail2.getAddress());
            String address = houseAddressDto.formatAddress();
            String etcAddress = houseAddressDto.formatEtcAddress();

            // 주택정보 입력
            LoadHouse house = new LoadHouse();
            house.setUserId(userId);
            house.setHouseType(SIX);
            house.setHouseName((houseAddressDto.formatEtcAddress().length() != 0 ? houseAddressDto.formatEtcAddress() : houseAddressDto.getSiDo() + "주택"));
            house.setSourceType(ONE);
            if (houseAddressDto.getAddressType() == 1) {
                house.setJibunAddr(address);
            } else {
                house.setRoadAddr(address);
            }
            house.setContractDate(LocalDate.parse(dataDetail2.getContractDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));
            house.setBalanceDate(LocalDate.parse(dataDetail2.getBalancePaymentDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));
            house.setBuyPrice(Long.parseLong(dataDetail2.getTradingPrice()));
            house.setArea(new BigDecimal(dataDetail2.getArea()));

            String keyword = address + SPACE + etcAddress;   // 주소검색을 위한 키워드

            // 주소기반산업지원서비스 API호출
            JusoGovRoadAdrResponse jusoGovRoadAdrResponse = jusoGovService.getRoadAdrInfo(address);
            int totalCount = Integer.parseInt(jusoGovRoadAdrResponse.getResults().getCommon().getTotalCount());
            log.debug("검색어 {}: 주소검색결과 {}건", address, totalCount);
            // 검색결과가 많은 경우 기타주소를 추가하여 검색
            if (totalCount > 1) {
                String keywordWithEtc = address + SPACE + etcAddress;
                jusoGovRoadAdrResponse = jusoGovService.getRoadAdrInfo(keywordWithEtc);
                totalCount = Integer.parseInt(jusoGovRoadAdrResponse.getResults().getCommon().getTotalCount());
                log.debug("재검색어 {}: 주소검색결과 {}건", keyword, totalCount);
            }

            if (totalCount == 1) {  // 주소기반 검색으로 매칭이 되는 경우 주택정보 세팅
                jusoDetail = jusoGovRoadAdrResponse.getResults().getJuso().get(0);
                jusoDetail = houseAddressService.replaceSpecialCharactersForJusoDetail(jusoDetail);

                // 주소기반에서 가져온 정보 주택정보 추가입력
                house.setHouseName(jusoDetail.getBdNm());
                house.setHouseTypeByName();
                house.setJibunAddr(jusoDetail.getJibunAddr());
                house.setRoadAddr(jusoDetail.getRoadAddr());
                house.setBdMgtSn(jusoDetail.getBdMgtSn());
                house.setAdmCd(jusoDetail.getAdmCd());
                house.setRnMgtSn(jusoDetail.getRnMgtSn());

                saveHouseInfo(userId, house);   // 보유주택정보 세션저장(테스트중..)
                boolean buildingComplete = false;      // 주택데이터 완성여부 판단(테스트중..)
                boolean propertyComplete = false;      // 주택데이터 완성여부 판단(테스트중..)

                // 주택정보 불러오기 API호출을 위한 생성
                HyphenUserSessionRequest hyphenUserSessionRequest = new HyphenUserSessionRequest();
                hyphenUserSessionRequest.setRoadAddress(house.getRoadAddr());
                hyphenUserSessionRequest.setJibunAddress(house.getJibunAddr());
                hyphenUserSessionRequest.setArea(house.getArea().toString());
                /*
                * ##########################################
                * 주택정보조회 API 직접 호출하여 정보 세팅(건축물대장)
                * /house/getHouseInfo
                * ##########################################
                */
                hyphenUserSessionRequest.setType(BUILDING);

                Object buildingResult = getHouseInfoForType(hyphenUserSessionRequest);

                if (buildingResult instanceof ApiResponse) {
                    ApiResponse<?> response = (ApiResponse<?>) buildingResult;
                    log.debug("building response: {}", response.getData());

                    if (NO.equals(response.getErrYn())) {
                        DataDetail1 dataDetail1 = (DataDetail1) response.getData();
                        if (dataDetail1 != null) {
                            house.setBuyDate(LocalDate.parse(dataDetail1.getOwnershipChangeDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));
                            house.setPubLandPrice(Long.parseLong(dataDetail1.getPublishedPrice())*1000);
                            buildingComplete = true;
                        }
                    }
                }

                /*
                * ##########################################
                * 주택정보조회 API 직접 호출하여 정보 세팅(재산세)
                * /house/getHouseInfo
                * ##########################################
                */
                hyphenUserSessionRequest.setType(PROPERTY);

                Object propertyResult = getHouseInfoForType(hyphenUserSessionRequest);
                if (propertyResult instanceof ApiResponse) {
                    ApiResponse<?> response = (ApiResponse<?>) propertyResult;
                    log.debug("property response: {}", response.toString());

                    if (NO.equals(response.getErrYn())) {
                        DataDetail3 dataDetail3 = (DataDetail3) response.getData();
                        if (dataDetail3 != null) {
                            HouseAddressDto houseAddressDto3 = houseAddressService.parseAddress(dataDetail3.getAddress());
                            house.setBuyDate(LocalDate.parse(dataDetail3.getAcquisitionDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));   //재산세 취득일자가 건축물대장보다 우선
                            house.setDetailAdr(houseAddressDto3.getDetailAddress());
                            // house.setComplete(true);
                            propertyComplete = true;
                        }
                    }
                }
                if (buildingComplete && propertyComplete) {
                    house.setComplete(true);
                }
            }

            houseList.add(house);
        }
        // 청약홈(하이픈)에서 가져온 주택 일괄삭제
        houseRepository.deleteByUserIdAndSourceType(findUser.getId(), ONE);

        return ApiResponse.success(houseList);
    }

    // 보유주택 목록 조회(DB)
    public Object getHouseList(String calcType) {
        log.info(">> [Service]HouseService getHouseList - 보유주택 목록 조회(DB)");

        List<House> houseListFromDB = houseUtil.findOwnHouseList();
        List<HouseSimpleInfoResponse> houseSimpleInfoResponseList = new ArrayList<>();

        if(houseListFromDB != null){
            for(House house : houseListFromDB){
                houseSimpleInfoResponseList.add(
                        HouseSimpleInfoResponse.builder()
                                .houseId(house.getHouseId())
                                .houseType(house.getHouseType())
                                .houseName(house.getHouseName())
                                .roadAddr(house.getRoadAddr())
                                .detailAdr(house.getDetailAdr())
                                .isMoveInRight(house.getIsMoveInRight())
                                .isRequiredDataMissing(checkOwnHouseRequiredDataMissing(house, calcType))
                                .build());
            }
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
                        .admCd(house.getAdmCd())
                        .rnMgtSn(house.getRnMgtSn())
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
                            .ownerCnt(1)
                            .userProportion(100)
                            .isMoveInRight(houseRegistRequest.getIsMoveInRight())
                            .isDestruction(false)
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

        if(houseId == null){
            log.error("수정 대상 주택 ID가 입력되지 않았습니다.");
            throw new CustomException(ErrorCode.HOUSE_MODIFY_ERROR, "수정 대상 주택 ID가 입력되지 않았습니다.");
        }
        
        Long userId = userUtil.findCurrentUser().getId();                       // 호출 사용자 ID
        House currentHouse = houseUtil.findSelectedHouse(houseId);              // 수정 대상 주택정보(AS-IS)
        Long houseOwnUserId = null;

        if(currentHouse != null){
            houseOwnUserId = currentHouse.getUserId();                          // 주택 소유자 ID
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
                            .sourceType(currentHouse.getSourceType())
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

    // 보유주택 일괄등록
    public Object saveAllHouse(HouseSaveAllRequest houseSaveAllRequest) throws Exception {
        log.info(">> HouseService saveAllHouse - 보유주택 일괄등록");

        Long userId = userUtil.findCurrentUser().getId();   // 호출 사용자

        if(houseSaveAllRequest == null){
            throw new CustomException(ErrorCode.HOUSE_REGIST_ERROR, "보유주택 일괄등록 정보가 입력되지 않았습니다.");
        }

        String calcType = StringUtils.defaultString(houseSaveAllRequest.getCalcType());
        List<HouseSaveRequest> houseSaveRequestList = houseSaveAllRequest.getHouseSaveRequestList();

        if(calcType == null){
            throw new CustomException(ErrorCode.HOUSE_REGIST_ERROR, "보유주택 일괄등록을 위한 계산 유형 값이 입력되지 않았습니다.");
        }

        if(houseSaveRequestList == null || houseSaveRequestList.isEmpty()){
            throw new CustomException(ErrorCode.HOUSE_REGIST_ERROR, "보유주택 일괄등록을 위한 등록할 주택이 입력되지 않았습니다.");
        }

        for(HouseSaveRequest houseSaveRequest : houseSaveRequestList){
            houseRepository.saveAndFlush(
                    House.builder()
                            .userId(userId)
                            .houseType(houseSaveRequest.getHouseType())
                            .houseName(houseSaveRequest.getHouseName())
                            .detailAdr(houseSaveRequest.getDetailAdr())
                            .contractDate(houseSaveRequest.getContractDate())
                            .balanceDate(houseSaveRequest.getBalanceDate())
                            .buyDate(houseSaveRequest.getBuyDate())
                            .buyPrice(houseSaveRequest.getBuyPrice())
                            .pubLandPrice(houseSaveRequest.getPubLandPrice())
                            .jibunAddr(houseSaveRequest.getJibunAddr())
                            .roadAddr(houseSaveRequest.getRoadAddr())
                            .roadAddrRef(houseSaveRequest.getRoadAddrRef())
                            .bdMgtSn(houseSaveRequest.getBdMgtSn())
                            .admCd(houseSaveRequest.getAdmCd())
                            .rnMgtSn(houseSaveRequest.getRnMgtSn())
                            .area(houseSaveRequest.getArea())
                            .ownerCnt(1)
                            .userProportion(100)
                            .isMoveInRight(false)
                            .isDestruction(false)
                            .sourceType(ONE)
                            .build());
        }

        // DB에 보유한 주택목록을 가져와서 응답값으로 세팅
        List<House> houseListFromDB = houseUtil.findOwnHouseList();
        List<HouseSimpleInfoResponse> houseSimpleInfoResponseList = new ArrayList<>();
        if(houseListFromDB != null){
            for(House house : houseListFromDB){
                houseSimpleInfoResponseList.add(
                        HouseSimpleInfoResponse.builder()
                                .houseId(house.getHouseId())
                                .houseType(house.getHouseType())
                                .houseName(house.getHouseName())
                                .detailAdr(house.getDetailAdr())
                                .isMoveInRight(house.getIsMoveInRight())
                                .isRequiredDataMissing(checkOwnHouseRequiredDataMissing(house,calcType))
                                .build());
            }
        }

        return ApiResponse.success(
                HouseListSearchResponse.builder()
                        .listCnt(houseSimpleInfoResponseList.size())
                        .list(houseSimpleInfoResponseList)
                        .build());
    }

    /*public Object saveAllHouse(List<House> houses) throws Exception {
        if (houses == null || houses.isEmpty()) {
            throw new CustomException(ErrorCode.HOUSE_REGIST_ERROR, "등록할 주택이 입력되지 않았습니다.");
        }

        // 입력받은 주택 일괄저장
        houses.forEach(house -> {
            house.setSourceType(ONE);
            house.setIsMoveInRight(false);
            house.setOwnerCnt(1);
            house.setUserProportion(100);
        });
        houseRepository.saveAll(houses);

        // DB에 보유한 주택목록을 가져와서 응답값으로 세팅
        List<House> houseListFromDB = houseUtil.findOwnHouseList();
        List<HouseSimpleInfoResponse> response = new ArrayList<>();
        if(houseListFromDB != null){
            for(House house : houseListFromDB){
                response.add(
                        HouseSimpleInfoResponse.builder()
                                .houseId(house.getHouseId())
                                .houseType(house.getHouseType())
                                .houseName(house.getHouseName())
                                .detailAdr(house.getDetailAdr())
                                .isMoveInRight(house.getIsMoveInRight())
                                .build());
            }
        }

        return ApiResponse.success(response);
    }*/

    // (양도주택)거주기간 조회
    public Object getHouseStayPeriod(HouseStayPeriodRequest houseStayPeriodRequest) throws Exception {
        log.info(">> [Service]HouseService getHouseStayPeriod - (양도주택)거주기간 조회");

        String[] activeProfiles = environment.getActiveProfiles();
        boolean isLocal = Arrays.asList(activeProfiles).contains("local");

        HyphenUserResidentRegistrationResponse hyphenUserResidentRegistrationResponse;
        if (isLocal) {
            // 로컬환경 : 하이픈 주택소유정보 테스트용 json data 세팅
            hyphenUserResidentRegistrationResponse = getMockedHyphenUserResidentRegistrationResponse();
        } else {
            // 그 외 개발/운영 등 : 하이픈 주택소유정보 조회 호출
            hyphenUserResidentRegistrationResponse
                = hyphenService.getUserStayPeriodInfo(houseStayPeriodRequest)
                .orElseThrow(() -> new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR));
        }

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
            // STEP2 : 주소가 양도주택과 동일한 경우(주소 분할하여 도로명주소 혹은 지번주소가 일치하고 상세주소도 일치하는지 확인)
            // STEP3 : 해당 주택의 '신고일'과 다음 건의 '신고일'의 날짜 차이를 계산
            // STEP4 : 날짜 차이를 응답 값의 '거주기간일자'에 세팅
            // STEP5 : 거주기간일자를 x년 y개월로 변경하여 응답값의 '거주기간정보'에 세팅
            // STEP6 : 해당 주택에 언제부터 언제까지 거주했는지를 정리하여 응답값의 '거주기간상세내용'에 세팅

            boolean hasStayInfo = false;                            // 거주정보존재여부
            String stayPeriodInfo = EMPTY;                          // 거주기간정보
            String stayPeriodCount = EMPTY;                         // 거주기간일자
            String ownPeriodDetail = EMPTY;                         // 보유기간 상세
            List<String> stayPeriodDetailList = new ArrayList<>();  // 거주기간 상세 리스트
            //String stayPeriodDetailContent = EMPTY;               // 거주기간상세내용

            long stayPeriodByDayCount = 0;
            long stayPeriodByMonthCount = 0;
            long stayPeriodByYearCount = 0;

            LocalDate buyDate = house.getBuyDate();
            LocalDate sellDate = houseStayPeriodRequest.getSellDate();

            ownPeriodDetail = buyDate + "부터 " + sellDate + "까지 보유";

            List<ChangeHistory> list = hyphenUserResidentRegistrationData.getChangeHistoryList();

            if(list != null){
                int index = 0;
                for(ChangeHistory history : list){
                    if(hasStayInfo) break;  // 거주정보존재여부가 true이면 반복문 종료

                    // STEP1 : 변동사유가 '전입'인 항목만 체크
                    if(MOVE_IN_KEYWORD.equals(history.getChangeReason())){
                        // 본인이 세대주인 경우만 체크 - 확인필요
                        // if(history.getHeadOfHouseHoldAndRelationShip().contains("본인")){
                        String address = StringUtils.defaultString(history.getAddress());
                            if(!EMPTY.equals(address)){
                                HouseAddressDto historyAddressDto = houseAddressService.separateAddress(address);
                                HouseAddressDto sellHouseAddressDto = null;

                                if(historyAddressDto.getAddressType() == 1){
                                    sellHouseAddressDto = houseAddressService.separateAddress(house.getJibunAddr());
                                } else {
                                    sellHouseAddressDto = houseAddressService.separateAddress(house.getRoadAddr());
                                }
                                // 동일주택여부 판단을 위한 상세주소 세팅
                                sellHouseAddressDto.setDetailAddress(house.getDetailAdr());

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
                                            boolean isStayCurrentSellHouse = false;

                                            // 다음 전입 신고일이 존재하는 경우
                                            if(!EMPTY.equals(nextReportDate)){
                                                nrDate = LocalDate.parse(nextReportDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                                            }
                                            // 다음 전입 신고일이 존재하지 않는 경우(해당 주택에서 현재까지 계속 거주하고 있는 경우)
                                            else{
                                                nrDate = LocalDate.now();
                                                isStayCurrentSellHouse = true;
                                            }

                                            // 보유기간 내에 포함되어있는 거주기간을 계산(누적)
                                            LocalDate startDate = (buyDate.isAfter(crDate)) ? buyDate : crDate;
                                            LocalDate endDate = (isStayCurrentSellHouse) ? sellDate : nrDate;

                                            stayPeriodByDayCount += ChronoUnit.DAYS.between(startDate, endDate);

                                            // 거주기간이 하루 이상은 되어야 세팅
                                            if(ChronoUnit.DAYS.between(crDate, nrDate) > 0){
                                                stayPeriodDetailList.add(crDate + "부터 " + nrDate + "까지 거주");
                                            }

                                            /*
                                            long stayPeriodByDay = ChronoUnit.DAYS.between(crDate, nrDate);
                                            long stayPeriodByMonth = ChronoUnit.MONTHS.between(crDate, nrDate);
                                            long stayPeriodByYear = ChronoUnit.YEARS.between(crDate, nrDate);

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
                                                stayPeriodDetailContent = crDate + "부터 " + nrDate + "까지 거주";
                                            }
                                            */
                                        }
                                    }
                                }
                            }
                        // }
                    }

                    index++;
                }

                stayPeriodByYearCount = stayPeriodByDayCount / PERIOD_YEAR;
                stayPeriodByMonthCount = (stayPeriodByDayCount - (stayPeriodByYearCount * PERIOD_YEAR)) / PERIOD_MONTH;

                if(stayPeriodByYearCount > 0){
                    stayPeriodInfo = stayPeriodByYearCount + "년 " + stayPeriodByMonthCount + "개월";
                }else{
                    stayPeriodInfo = stayPeriodByMonthCount + "개월";
                }

                stayPeriodCount = stayPeriodByDayCount + "일";

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
                    .ownPeriodDetail(ownPeriodDetail)
                    .stayPeriodDetailList(stayPeriodDetailList)
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
                                .buyDate(LocalDate.parse(dataDetail1.getOwnershipChangeDate(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .pubLandPrice(Long.parseLong(publishedPrice)*1000)      // 공시가격(단위:천원)
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

            // 세팅된 주택이 거래내역 매도 목록에 있는지 확인하여 제외
            Iterator<House> iterator = houseList.iterator();
            while (iterator.hasNext()) {
                House house = iterator.next();
                for (DataDetail2 dataDetail2 : list) {
                    HouseAddressDto houseAddressDto = houseAddressService.separateAddress(dataDetail2.getAddress());
                    String tradeType = this.getTradeTypeFromSellBuyClassification(StringUtils.defaultString(dataDetail2.getSellBuyClassification()));
                    // 거래내역 [매도] 건이면 세팅된 주택에서 주소를 비교하여 제거
                    if(TWO.equals(tradeType)){
                        if (houseAddressService.compareAddress(houseAddressDto, house)) {
                            log.info("제거할 주택 정보 : {}", house.toString());
                            iterator.remove();
                            break;
                        }
                    }
                }
            }

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
        List<String> searchAdr = hyphenUserHouseResultInfo.getSearchAdr();
        int count = 0;

        if(searchAdr != null){
            count = Math.min(MAX_JUSO_CALL_CNT, searchAdr.size());
            StringBuilder searchAddr = new StringBuilder(EMPTY);

            // 주소기반산업지원서비스 도로명주소 검색은 한 건당 최대 (3)회 까지로 제한
            for(int i=0; i<count; i++){
                if(searchAdr.get(i) != null){
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
                                // 검색 결과가 2건인 경우 : 대단지의 경우 로직에 대한 검토가 더 필요함..
                                else if(ttcn == 2){
                                    // CASE1) 공동주택여부 판별하여 세팅
                                    for(int j = 0; j < ttcn; j++) {
                                        String bdkdCd = jusoGovRoadAdrResponse.getResults().getJuso().get(j).getBdKdcd();
                                        if (ONE.equals(bdkdCd)) {
                                            jusoDetail = jusoGovRoadAdrResponse.getResults().getJuso().get(j);
                                            jusoDetail = houseAddressService.replaceSpecialCharactersForJusoDetail(jusoDetail);
                                            break;
                                        }
                                    }
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
                    // if(house.getRoadAddrRef().isBlank()) house.setRoadAddrRef(jusoDetail.getRoadAddrPart2());
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
                                    .roadAddr(jusoDetail.getRoadAddr())
                                    //.roadAddrRef(jusoDetail.getRoadAddrPart2())
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
                                    .roadAddr(jusoDetail.getRoadAddr())
                                    //.roadAddrRef(jusoDetail.getRoadAddrPart2())
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
                                    .roadAddr(jusoDetail.getRoadAddr())
                                    //.roadAddrRef(jusoDetail.getRoadAddrPart2())
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
            // 취득계약일자, 취득일자 또는 취득금액 데이터가 없으면 필수데이터 누락 '여'
            if(house.getContractDate() == null || house.getBuyDate() == null || house.getBuyPrice() == null){
                log.info("양도소득세 계산의 보유주택조회인 경우 - 계약일자, 취득일자 또는 취득가격 데이터가 없으면 필수데이터 누락");
                isRequiredDataMissing = true;
            }
        }
        log.info("isRequiredDataMissing : " + isRequiredDataMissing);

        return isRequiredDataMissing;
    }

    // 청약홈에서 불러온 보유주택 누락된 정보 입력
    private void fillEmptyValuesFromLists(List<DataDetail1> list1, List<DataDetail2> list2, List<DataDetail3> list3, List<House> houseList){
        log.info(">> HouseService fillEmptyValuesFromLists - 주택정보 빈값 채워넣는 로직");
        // 1. 부동산거래내역이 1개인 경우에는 주소비교 없이 세팅
        // 2. 거래내역이 여러건인 경우 거래유형 [매수]
        // 3. 거래유형 [매수] 중 보유주택 주소와 유사한 경우 세팅

        HyphenUserHouseResultInfo hyphenUserHouseResultInfo = new HyphenUserHouseResultInfo();
        
        // 부동산거래내역
        for (int i = 0; i < list2.size(); i++) {
            DataDetail2 dataDetail2 = list2.get(i);

            String tradeType = this.getTradeTypeFromSellBuyClassification(StringUtils.defaultString(dataDetail2.getSellBuyClassification()));
            String buyPrice = ZERO;
            String myHouseAddress = EMPTY;

            if (list2.size() == 1) {
                buyPrice = StringUtils.defaultString(dataDetail2.getTradingPrice(), ZERO);

                hyphenUserHouseResultInfo.setBuyPrice(Long.parseLong(buyPrice));
                hyphenUserHouseResultInfo.setContractDate(LocalDate.parse(dataDetail2.getContractDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));

                if (houseList.get(0).getBuyPrice() == null) {
                    houseList.get(0).setBuyPrice(hyphenUserHouseResultInfo.getBuyPrice());
                }
                if (houseList.get(0).getContractDate() == null) {
                    houseList.get(0).setContractDate(hyphenUserHouseResultInfo.getContractDate());
                }
            } else {
                // 거래유형이 매수인 경우 보유주택과 비교
                if(ONE.equals(tradeType)) {
                    HouseAddressDto houseAddressDto = houseAddressService.separateAddress(dataDetail2.getAddress());
                    log.info("{}개 거래내역 중 {}번째 매수 주택: {}", list2.size(), i+1, houseAddressDto.getSearchAddress().get(0));

                    for (House house : houseList) {
                        if (houseAddressDto.getAddressType() == 1) {
                            log.info("보유주택 지번주소: {}", house.getJibunAddr());
                            myHouseAddress = house.getJibunAddr();
                        } else {
                            log.info("보유주택 도로명주소: {}", house.getRoadAddr());
                            myHouseAddress = house.getRoadAddr();
                        }
                        
                        // 부동산거래내역의 주소가 보유주택 주소와 일부 일치하는 경우 [취득금액]과 [계약일자] 세팅
                        if (houseAddressService.compareAddress(houseAddressDto, house)) {
                        //if (myHouseAddress.contains(houseAddressDto.getSearchAddress().get(0))) {
                            log.debug("주소의 일부가 일치함!", myHouseAddress);
                            buyPrice = StringUtils.defaultString(dataDetail2.getTradingPrice(), ZERO);

                            hyphenUserHouseResultInfo.setBuyPrice(Long.parseLong(buyPrice));
                            hyphenUserHouseResultInfo.setContractDate(LocalDate.parse(dataDetail2.getContractDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));

                            if (house.getBuyPrice() == null) {
                                house.setBuyPrice(hyphenUserHouseResultInfo.getBuyPrice());
                            }
                            if (house.getContractDate() == null) {
                                house.setContractDate(hyphenUserHouseResultInfo.getContractDate());
                            }

                            break;
                        }
                    }
                }
            }
        }
        // 재산세정보
        for (int i = 0; i < list3.size(); i++) {
            DataDetail3 dataDetail3 = list3.get(i);
            String myHouseAddress = EMPTY;

            HouseAddressDto houseAddressDto = houseAddressService.separateAddress(dataDetail3.getAddress());
            log.info("{}개 재산세정보 중 {}번째 정보: {}", list3.size(), i+1, houseAddressDto.getSearchAddress().get(0));

            for (House house : houseList) {
                if (houseAddressDto.getAddressType() == 1) {
                    log.info("보유주택 지번주소: {}", house.getJibunAddr());
                    myHouseAddress = house.getJibunAddr();
                } else {
                    log.info("보유주택 도로명주소: {}", house.getRoadAddr());
                    myHouseAddress = house.getRoadAddr();
                }
                
                if (myHouseAddress.contains(houseAddressDto.getSearchAddress().get(0))) {
                    hyphenUserHouseResultInfo.setDetailAdr(houseAddressDto.getDetailAddress());
                    hyphenUserHouseResultInfo.setBuyDate(LocalDate.parse(dataDetail3.getAcquisitionDate(), DateTimeFormatter.ofPattern("yyyyMMdd")));

                    if (house.getDetailAdr() == null) {
                        house.setDetailAdr(hyphenUserHouseResultInfo.getDetailAdr());
                    }
                    if (house.getBuyDate() == null) {
                        house.setBuyDate(hyphenUserHouseResultInfo.getBuyDate());
                    }
                    break;
                }
            }
        }
    }

    /*
    * ##########################################
    * 거래내역 리스트에서 매수거래만 추출하기
    * - 추후 주소비교로직 추가 수정 필요할 수 있음
    * ##########################################
    */
    private List<DataDetail2> createBuyTransactions(List<DataDetail2> list2) {
        Set<String> sellAddresses = new HashSet<>();            // 매도 거래 세트
        List<DataDetail2> buyTransactions = new ArrayList<>();  // 매수 거래 리스트

        for (DataDetail2 dataDetail2 : list2) {
            HouseAddressDto houseAddressDto = houseAddressService.parseAddress(dataDetail2.getAddress());
            String address = houseAddressDto.formatAddress();
            String tradeType = getTradeTypeFromSellBuyClassification(dataDetail2.getSellBuyClassification());
            LocalDate balancePaymentDate = LocalDate.parse(dataDetail2.getBalancePaymentDate(), DateTimeFormatter.ofPattern("yyyyMMdd"));

            if (TWO.equals(tradeType)) {
                // 매도 거래 주소를 세트에 추가
                sellAddresses.add(address);
            } else if (ONE.equals(tradeType)) {
                // 매수 거래인 경우 매도 거래 주소 목록에 포함되지 않은 경우에 매수 거래 리스트에 추가
                if (!sellAddresses.contains(address)) {
                    // 잔금일자가 오늘보다 미래인 경우 매수주택에 포함하지 않음
                    if (balancePaymentDate.isBefore(LocalDate.now())) {
                        buyTransactions.add(dataDetail2);
                    } else {
                        log.debug("잔금일자가 도래하지 않은 매수거래내역 (매수일자: {})", balancePaymentDate);
                    }
                }
            }
        }

        return buyTransactions;
    }

    /*
    * ##########################################
    * 세션에서 동일한 주소정보 검색
    * @Input  : 검색할 타입, 도로명주소, 지번주소, 면적
    * @Output : 일치하는 건축물대장 정보
    * description : 비교할 수 있는 정보가 주소와 면적밖에 없음
    * ##########################################
    */
    public Object getHouseInfoForType(HyphenUserSessionRequest hyphenUserSessionRequest) {
        if (EMPTY.equals(hyphenUserSessionRequest.getArea().trim())) {
            return ApiResponse.error(ErrorCode.HOUSE_GET_INFO_ERROR.getMessage(), "면적이 입력되지 않았습니다.", ErrorCode.HOUSE_GET_INFO_ERROR.getCode());
        }
        if (EMPTY.equals(hyphenUserSessionRequest.getType())) {
            return ApiResponse.error(ErrorCode.HOUSE_GET_INFO_ERROR.getMessage(), "조회 타입이 입력되지 않았습니다.", ErrorCode.HOUSE_GET_INFO_ERROR.getCode());
        }
        // 로그인 사용자 정보 가져오기
        User findUser = userUtil.findCurrentUser();
        Long userId = findUser.getId();

        String type = hyphenUserSessionRequest.getType();
        BigDecimal compareArea;
        try {
            compareArea  = new BigDecimal(hyphenUserSessionRequest.getArea());    // 면적
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.HOUSE_GET_INFO_ERROR.getMessage(), "면적이 잘못 입력되었습니다.", ErrorCode.HOUSE_GET_INFO_ERROR.getCode());
        }

        HouseAddressDto roadAddressDto = houseAddressService.parseAddress(hyphenUserSessionRequest.getRoadAddress());
        HouseAddressDto jibunAddressDto = houseAddressService.parseAddress(hyphenUserSessionRequest.getJibunAddress());

        // 세션개수 가져오기
        int cnt = redisService.countKey(userId, type);
        // 세션에서 저장된 주소와 입력받은 주소 비교
        for (int i = 0; i < cnt; i++) {
            log.debug("총 개수 {}개 중 {}번째", cnt, i+1);
            Map<Object, Object> getRedis = redisService.getHashMap(userId, type, String.valueOf(i + 1));
            String address = getRedis.get("address").toString();
            BigDecimal area = new BigDecimal(getRedis.get("area").toString());
            
            // 세션에서 가져온 주소 분할
            HouseAddressDto houseAddressDto = houseAddressService.parseAddress(address);
            // 주소비교 후 면적까지 비교 - 추후 메소드 분리 필요
            if (houseAddressDto.getAddressType() == 1) {
                // 지번주소 비교
                if (houseAddressDto.isSameAddress(jibunAddressDto)) {
                    log.debug("지번주소 일치함");
                    // 입력받은 주소의 면적과 세션에 저장된 면적을 비교
                    String houseArea = compareArea.setScale(0, RoundingMode.DOWN).toString();
                    String sessionArea = area.setScale(0, RoundingMode.DOWN).toString();
                    if (houseArea.equals(sessionArea)) {
                        return getMatchingResponse(type, getRedis);
                    }
                }
            } else {
                // 도로명주소 비교
                if (houseAddressDto.isSameAddress(roadAddressDto)) {
                    log.debug("도로명주소 일치함");
                    // 입력받은 주소의 면적과 세션에 저장된 면적을 비교
                    String houseArea = compareArea.setScale(0, RoundingMode.DOWN).toString();
                    String sessionArea = area.setScale(0, RoundingMode.DOWN).toString();
                    if (houseArea.equals(sessionArea)) {
                        return getMatchingResponse(type, getRedis);
                    }
                }
            }
        }

        return ApiResponse.success(null);
    }
    private ApiResponse<?> getMatchingResponse(String type, Map<Object, Object> redisData) {
        if (BUILDING.equals(type)) {
            // 건축물대장 DTO생성 및 데이터 세팅
            DataDetail1 dataDetail1 = new DataDetail1();
            dataDetail1.setAddress(redisData.get("address").toString());
            dataDetail1.setArea(redisData.get("area").toString());
            dataDetail1.setApprovalDate(redisData.get("approvalDate").toString());
            dataDetail1.setReasonChangeOwnership(redisData.get("reasonChangeOwnership").toString());
            dataDetail1.setOwnershipChangeDate(redisData.get("ownershipChangeDate").toString());
            dataDetail1.setBaseDate(redisData.get("baseDate").toString());
            dataDetail1.setPublicationBaseDate(redisData.get("publicationBaseDate").toString());
            dataDetail1.setPublishedPrice(redisData.get("publishedPrice").toString());

            return ApiResponse.success(dataDetail1);
            // return dataDetail1;
        } else if (PROPERTY.equals(type)) {
            // 재산세내역 DTO생성 및 데이터 세팅
            DataDetail3 dataDetail3 = new DataDetail3();
            dataDetail3.setAddress(redisData.get("address").toString());
            dataDetail3.setArea(redisData.get("area").toString());
            dataDetail3.setAcquisitionDate(redisData.get("acquisitionDate").toString());
            dataDetail3.setBaseDate(redisData.get("baseDate").toString());

            return ApiResponse.success(dataDetail3);
        }
        return ApiResponse.error(ErrorCode.HOUSE_GET_INFO_ERROR.getMessage(), "조회타입 입력이 잘못되었습니다.", ErrorCode.HOUSE_GET_INFO_ERROR.getCode());
    }

    // 재산세, 건축물대장 한번에 조회
    public Object getHouseInfo(HyphenUserSessionRequest hyphenUserSessionRequest) {
        Map<String, Object> combinedInfo = new LinkedHashMap<>();
        log.info("조회할 주소: {}", hyphenUserSessionRequest.getRoadAddress());

        hyphenUserSessionRequest.setType(BUILDING);
        ApiResponse<?> buildingRes = (ApiResponse<?>) getHouseInfoForType(hyphenUserSessionRequest);
        if (NO.equals(buildingRes.getErrYn())) {
            Object dataDetail1 = buildingRes.getData();
            if (dataDetail1 instanceof DataDetail1) {
                combinedInfo.put("building", dataDetail1);
            }
        } else {
            return ApiResponse.error(buildingRes.getErrMsg(), buildingRes.getErrMsgDtl(), buildingRes.getErrCode());
        }
        
        hyphenUserSessionRequest.setType(PROPERTY);
        ApiResponse<?> propertyRes = (ApiResponse<?>) getHouseInfoForType(hyphenUserSessionRequest);
        if (NO.equals(propertyRes.getErrYn())) {
            Object dataDetail3 = propertyRes.getData();
            if (dataDetail3 instanceof DataDetail3) {
                combinedInfo.put("property", dataDetail3);
            }
        } else {
            return ApiResponse.error(propertyRes.getErrMsg(), propertyRes.getErrMsgDtl(), propertyRes.getErrCode());
        }

        return ApiResponse.success(combinedInfo);
    }

    // 하이픈 건축물대장정보 redis 저장
    public void saveBuildingInfo(Long userId, List<DataDetail1> list) {
        redisService.deleteHashMap(userId, BUILDING);  // 기존 세션값 초기화

        for (int i = 0; i < list.size(); i++) {
            Map<String, String> propertyInfo = new HashMap<>();
            propertyInfo.put("address", list.get(i).getAddress());
            propertyInfo.put("area", list.get(i).getArea());
            propertyInfo.put("approvalDate", list.get(i).getApprovalDate());
            propertyInfo.put("reasonChangeOwnership", list.get(i).getReasonChangeOwnership());
            propertyInfo.put("ownershipChangeDate", list.get(i).getOwnershipChangeDate());
            propertyInfo.put("publicationBaseDate", list.get(i).getPublicationBaseDate());
            propertyInfo.put("publishedPrice", list.get(i).getPublishedPrice());
            propertyInfo.put("baseDate", list.get(i).getBaseDate());

            redisService.saveHashMap(userId, BUILDING, String.valueOf(i + 1), propertyInfo);
        }
    }

    // 하이픈 부동산거래내역 redis 저장
    public void saveTradingInfo(Long userId, List<DataDetail2> list) {
        redisService.deleteHashMap(userId, TRADE);  // 기존 세션값 초기화

        for (int i = 0; i < list.size(); i++) {
            Map<String, String> propertyInfo = new HashMap<>();
            propertyInfo.put("address", list.get(i).getAddress());
            propertyInfo.put("sellBuyClassification", list.get(i).getSellBuyClassification());
            propertyInfo.put("area", list.get(i).getArea());
            propertyInfo.put("tradingPrice", list.get(i).getTradingPrice());
            propertyInfo.put("balancePaymentDate", list.get(i).getBalancePaymentDate());
            propertyInfo.put("contractDate", list.get(i).getContractDate());
            propertyInfo.put("startDate", list.get(i).getStartDate());
            propertyInfo.put("endDate", list.get(i).getEndDate());

            redisService.saveHashMap(userId, TRADE, String.valueOf(i + 1), propertyInfo);
        }
    }

    // 하이픈 재산세정보 redis 저장
    public void savePropertyInfo(Long userId, List<DataDetail3> list) {
        redisService.deleteHashMap(userId, PROPERTY);  // 기존 세션값 초기화

        for (int i = 0; i < list.size(); i++) {
            Map<String, String> propertyInfo = new HashMap<>();
            propertyInfo.put("address", list.get(i).getAddress());
            propertyInfo.put("area", list.get(i).getArea());
            propertyInfo.put("acquisitionDate", list.get(i).getAcquisitionDate());
            propertyInfo.put("baseDate", list.get(i).getBaseDate());

            redisService.saveHashMap(userId, PROPERTY, String.valueOf(i + 1), propertyInfo);
        }
    }

    // 보유주택정보 redis 저장
    public void saveHouseInfo(Long userId, House house) {
        Map<String, String> houseInfo = new HashMap<>();
        houseInfo.put("bdMgtSn", house.getBdMgtSn());
        houseInfo.put("houseName", house.getHouseName());
        houseInfo.put("jibunAddr", house.getJibunAddr());
        houseInfo.put("roadAddr", house.getRoadAddr());
        houseInfo.put("contractDate", house.getContractDate().toString());
        houseInfo.put("balbanceDate", house.getBalanceDate().toString());
        houseInfo.put("buyPriace", house.getBuyPrice().toString());
        houseInfo.put("area", house.getArea().toString());

        // 건물관리번호가 key인데 동일 주택 2채 보유의 경우 고려 필요할듯
        redisService.saveHashMap(userId, "house", house.getBdMgtSn(), houseInfo);
    }

    // 건축물대장 기준 매매 외 취득주택 추출
    public Object getEtcHouse() {
        Long userId = userUtil.findCurrentUserId();

        // DB에서 보유주택 목록 가져오기
        List<House> houseListFromDB = houseUtil.findOwnHouseList();
        List<LoadHouse> etcHouseList = new ArrayList<>();
        
        // redis에서 재산세 주택 가져오기
        int cnt = redisService.countKey(userId, PROPERTY);
        for (int i = 0; i < cnt; i++) {
            log.info("총 개수 {}개 중 {}번째", cnt, i+1);
            Map<Object, Object> getRedis = redisService.getHashMap(userId, PROPERTY, String.valueOf(i + 1));
            String address = getRedis.get("address").toString();

            LoadHouse etcHouse = new LoadHouse();   // 재산세 내역 중 기타주소 담을 엔티티
            etcHouse.setUserId(userId);

            // 가져온 주택정보 HouseAddressDto로 전환
            HouseAddressDto houseAddressDto = houseAddressService.parseAddress(address);
            if (houseAddressDto.getAddressType() == 1) {
                etcHouse.setJibunAddr(houseAddressDto.formatAddress());
            } else {
                etcHouse.setRoadAddr(houseAddressDto.formatAddress());
            }
            etcHouse.setArea(new BigDecimal(getRedis.get("area").toString()));
            etcHouse.setDetailAdr(houseAddressDto.getDetailAddress());
            etcHouse.setHouseName((!houseAddressDto.formatEtcAddress().isEmpty() ? houseAddressDto.formatEtcAddress() : houseAddressDto.getSiDo() + "주택"));
            etcHouse.setHouseTypeByName();
            etcHouse.setSourceType(ONE);
            etcHouse.setComplete(false);

            // 주소비교
            boolean isMatched = false;
            if(houseListFromDB != null){
                isMatched = houseListFromDB.stream().anyMatch(house -> {
                    HouseAddressDto hasHouseAddressDto;
                    if (houseAddressDto.getAddressType() == 1) {
                        hasHouseAddressDto = houseAddressService.parseAddress(house.getJibunAddr());
                    } else {
                        hasHouseAddressDto = houseAddressService.parseAddress(house.getRoadAddr());
                    }
                    log.debug("보유주택과 주소비교: {}, {}", houseAddressDto.formatAddress(), hasHouseAddressDto.formatAddress());
                    return houseAddressDto.isSameAddress(hasHouseAddressDto);
                });
            }

            // 일치하지 않으면 기타주소 리스트에 추가
            if (!isMatched) {
                etcHouseList.add(etcHouse);
                log.info("기타 주택으로 추가: {}", etcHouse.getHouseName());
            }
        }

        return ApiResponse.success(etcHouseList);
    }

    private HyphenUserHouseListResponse getMockedHyphenUserHouseListResponse(String userNm) {
        // 하이픈 응답값 세팅
        String json;
        log.debug("userNm: {}", userNm);
        if ("일번".equals(userNm)) {
            // 1주택 : 모든 주소 정상 : 매핑성공
            json = "{\"common\":{\"userTrNo\":\"\",\"hyphenTrNo\":\"10202407290000042367\",\"errYn\":\"N\",\"errMsg\":\"\"},\"data\":{\"listMsg1\":\"\",\"list1\":[{\"name\":\"이***\",\"address\":\"서울특별시 강서구 화곡로 186-22 (화곡동)\",\"area\":\"26.84\",\"approvalDate\":\"20170704\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20230425\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"139000\",\"baseDate\":\"20240729\"}],\"listMsg2\":\"\",\"list2\":[{\"name\":\"이***\",\"address\":\"서울특별시 강서구 화곡동 143-11 성재센트리움아파트-****호\",\"sellBuyClassification\":\"매수(일반)\",\"area\":\"26.84\",\"tradingPrice\":\"235000000\",\"balancePaymentDate\":\"20230426\",\"contractDate\":\"20230420\",\"startDate\":\"20060101\",\"endDate\":\"20240729\"}],\"listMsg3\":\"\",\"list3\":[{\"name\":\"이***\",\"address\":\"서울특별시 강서구 화곡동 143-11 성재센트리움아파트                                 0000동 00902호\",\"area\":\"26.84\",\"acquisitionDate\":\"20230425\",\"baseDate\":\"20240327\"}]}}";
        } else if("이번".equals(userNm)) {
            // 1주택 : 거래내역 주소 비정상 + 그 외 주소 정상 : 사용자 주소입력
            json = "{\"common\":{\"userTrNo\":\"\",\"hyphenTrNo\":\"10202408050000026237\",\"errYn\":\"N\",\"errMsg\":\"\"},\"data\":{\"listMsg1\":\"\",\"list1\":[{\"name\":\"정**\",\"address\":\"강원특별자치도 속초시 동해대로 3961 (조양동)\",\"area\":\"77.07\",\"approvalDate\":\"20210730\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20240124\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"204000\",\"baseDate\":\"20240805\"},{\"name\":\"정**\",\"address\":\"충청북도 청주시 흥덕구 송화로214번길 29 (송절동)\",\"area\":\"84.9365\",\"approvalDate\":\"20180726\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20181026\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"261000\",\"baseDate\":\"20240805\"}],\"listMsg2\":\"\",\"list2\":[{\"name\":\"정**\",\"address\":\"충청북도 청주흥덕구 송절동 BL-A-5 청주 테크노폴리스 우미린 ****동-****호\",\"sellBuyClassification\":\"매수(분양권(공급계약))\",\"area\":\"84.9365\",\"tradingPrice\":\"296800000\",\"balancePaymentDate\":\"20180801\",\"contractDate\":\"20170731\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"}],\"listMsg3\":\"\",\"list3\":[{\"name\":\"정**\",\"address\":\"충청북도 청주흥덕구 송절동 850 테크노폴리스 우미린                                0104동 00801호\",\"area\":\"84.93\",\"acquisitionDate\":\"20180810\",\"baseDate\":\"20240327\"}]}}";
        } else if("삼번".equals(userNm)) {
            // 1주택 : 거래내역 주소 비정상 + 건물명 없음 + 재산세 내역 없음
            json = "{\"common\":{\"userTrNo\":\"\",\"hyphenTrNo\":\"10202408010000023421\",\"errYn\":\"N\",\"errMsg\":\"\"},\"data\":{\"listMsg1\":\"\",\"list1\":[{\"name\":\"이**\",\"address\":\"울산광역시 동구 서부로 51 (서부동)\",\"area\":\"84.9839\",\"approvalDate\":\"20230526\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20231005\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"-\",\"baseDate\":\"20240801\"}],\"listMsg2\":\"\",\"list2\":[{\"name\":\"이**\",\"address\":\"울산광역시 동구 서부동 248-37 울산 지웰시티 자이 2단지 ****동-****호\",\"sellBuyClassification\":\"매수(분양권(전매))\",\"area\":\"84.9839\",\"tradingPrice\":\"555343400\",\"balancePaymentDate\":\"20220615\",\"contractDate\":\"20220607\",\"startDate\":\"20060101\",\"endDate\":\"20240801\"}],\"listMsg3\":\"\",\"list3\":[]}}";
        } else {
            // 2주택 : 거래내역 주소 정상 + 거래내역 주소 비정상 + 거래내역 없는 재산세 1건 : 1건 매핑성공, 1건 주소입력 + 매매 외 주택 확인
            // json = "{\"common\":{\"userTrNo\":\"\",\"hyphenTrNo\":\"10202408050000023444\",\"errYn\":\"N\",\"errMsg\":\"\"},\"data\":{\"listMsg1\":\"\",\"list1\":[{\"name\":\"고**\",\"address\":\"강원특별자치도 원주시 서원대로 290 (명륜동)\",\"area\":\"84.9836\",\"approvalDate\":\"20211129\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20220317\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"245000\",\"baseDate\":\"20240805\"},{\"name\":\"정**\",\"address\":\"충청북도 청주시 흥덕구 송화로214번길 29 (송절동)\",\"area\":\"84.9365\",\"approvalDate\":\"20180726\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20181026\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"261000\",\"baseDate\":\"20240805\"}],\"listMsg2\":\"\",\"list2\":[{\"name\":\"고**\",\"address\":\"강원특별자치도 원주시 명륜동 산31 원주 더샵 센트럴파크 2단지 ****동-****호\",\"sellBuyClassification\":\"매수(분양권(공급계약))\",\"area\":\"84.9836\",\"tradingPrice\":\"334000000\",\"balancePaymentDate\":\"20220131\",\"contractDate\":\"20191214\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"서울특별시 도봉구 창동 825 북한산아이파크 ****동-****호\",\"sellBuyClassification\":\"매도(일반)\",\"area\":\"84.4516\",\"tradingPrice\":\"680000000\",\"balancePaymentDate\":\"20200207\",\"contractDate\":\"20191117\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"경기도 의왕시 포일동 643 위브호수마을2단지 ****동-****호\",\"sellBuyClassification\":\"매도(일반)\",\"area\":\"80.174\",\"tradingPrice\":\"555000000\",\"balancePaymentDate\":\"20200103\",\"contractDate\":\"20191018\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"경기도 의왕시 포일동 643 두산위브호수마을2단지 ****동-****호\",\"sellBuyClassification\":\"매수(일반)\",\"area\":\"80.174\",\"tradingPrice\":\"426000000\",\"balancePaymentDate\":\"20170331\",\"contractDate\":\"20170329\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"서울특별시 도봉구 창동 825 북한산아이파크 ****동-****호\",\"sellBuyClassification\":\"매수(기존주택)\",\"area\":\"84.4516\",\"tradingPrice\":\"437000000\",\"balancePaymentDate\":\"20160610\",\"contractDate\":\"20160409\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"정**\",\"address\":\"충청북도 청주흥덕구 송절동 BL-A-5 청주 테크노폴리스 우미린 ****동-****호\",\"sellBuyClassification\":\"매수(분양권(공급계약))\",\"area\":\"84.9365\",\"tradingPrice\":\"296800000\",\"balancePaymentDate\":\"20180801\",\"contractDate\":\"20170731\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"}],\"listMsg3\":\"\",\"list3\":[{\"name\":\"고**\",\"address\":\"강원특별자치도 원주시 명륜동 856 더샵 원주센트럴파크 2단지                          0207동 00401호\",\"area\":\"84.98\",\"acquisitionDate\":\"20220226\",\"baseDate\":\"20240327\"},{\"name\":\"이**\",\"address\":\"경상남도 김해시 율하동 1297 율상마을 푸르지오3단지                             0305동 00401호\",\"area\":\"84.96\",\"acquisitionDate\":\"20210112\",\"baseDate\":\"20240327\"},{\"name\":\"정**\",\"address\":\"충청북도 청주흥덕구 송절동 850 테크노폴리스 우미린                                0104동 00801호\",\"area\":\"84.93\",\"acquisitionDate\":\"20180810\",\"baseDate\":\"20240327\"},{\"name\":\"김**\",\"address\":\"경기도 의왕시 내손동 845 인덕원 센트럴 자이                                 0206동 00401호\",\"area\":\"84.98\",\"acquisitionDate\":\"20181102\",\"baseDate\":\"20240327\"}]}}";
            json = "{\"common\":{\"userTrNo\":\"\",\"hyphenTrNo\":\"10202408050000023444\",\"errYn\":\"N\",\"errMsg\":\"\"},\"data\":{\"listMsg1\":\"\",\"list1\":[{\"name\":\"고**\",\"address\":\"강원특별자치도 원주시 서원대로 290 (명륜동)\",\"area\":\"84.9836\",\"approvalDate\":\"20211129\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20220317\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"245000\",\"baseDate\":\"20240805\"},{\"name\":\"정**\",\"address\":\"충청북도 청주시 흥덕구 송화로214번길 29 (송절동)\",\"area\":\"84.9365\",\"approvalDate\":\"20180726\",\"reasonChangeOwnership\":\"소유권이전\",\"ownershipChangeDate\":\"20181026\",\"publicationBaseDate\":\"20230101\",\"publishedPrice\":\"261000\",\"baseDate\":\"20240805\"}],\"listMsg2\":\"\",\"list2\":[{\"name\":\"고**\",\"address\":\"서울특별시 도봉구 창동 825 북한산아이파크 ****동-****호\",\"sellBuyClassification\":\"매도(일반)\",\"area\":\"84.4516\",\"tradingPrice\":\"680000000\",\"balancePaymentDate\":\"20200207\",\"contractDate\":\"20191117\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"경기도 의왕시 포일동 643 위브호수마을2단지 ****동-****호\",\"sellBuyClassification\":\"매도(일반)\",\"area\":\"80.174\",\"tradingPrice\":\"555000000\",\"balancePaymentDate\":\"20200103\",\"contractDate\":\"20191018\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"경기도 의왕시 포일동 643 두산위브호수마을2단지 ****동-****호\",\"sellBuyClassification\":\"매수(일반)\",\"area\":\"80.174\",\"tradingPrice\":\"426000000\",\"balancePaymentDate\":\"20170331\",\"contractDate\":\"20170329\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"고**\",\"address\":\"서울특별시 도봉구 창동 825 북한산아이파크 ****동-****호\",\"sellBuyClassification\":\"매수(기존주택)\",\"area\":\"84.4516\",\"tradingPrice\":\"437000000\",\"balancePaymentDate\":\"20160610\",\"contractDate\":\"20160409\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"},{\"name\":\"정**\",\"address\":\"충청북도 청주흥덕구 송절동 BL-A-5 청주 테크노폴리스 우미린 ****동-****호\",\"sellBuyClassification\":\"매수(분양권(공급계약))\",\"area\":\"84.9365\",\"tradingPrice\":\"296800000\",\"balancePaymentDate\":\"20180801\",\"contractDate\":\"20170731\",\"startDate\":\"20060101\",\"endDate\":\"20240805\"}],\"listMsg3\":\"\",\"list3\":[{\"name\":\"고**\",\"address\":\"강원특별자치도 원주시 명륜동 856 더샵 원주센트럴파크 2단지                          0207동 00401호\",\"area\":\"84.98\",\"acquisitionDate\":\"20220226\",\"baseDate\":\"20240327\"},{\"name\":\"이**\",\"address\":\"경상남도 김해시 율하동 1297 율상마을 푸르지오3단지                             0305동 00401호\",\"area\":\"84.96\",\"acquisitionDate\":\"20210112\",\"baseDate\":\"20240327\"},{\"name\":\"정**\",\"address\":\"충청북도 청주흥덕구 송절동 850 테크노폴리스 우미린                                0104동 00801호\",\"area\":\"84.93\",\"acquisitionDate\":\"20180810\",\"baseDate\":\"20240327\"},{\"name\":\"김**\",\"address\":\"경기도 의왕시 내손동 845 인덕원 센트럴 자이                                 0206동 00401호\",\"area\":\"84.98\",\"acquisitionDate\":\"20181102\",\"baseDate\":\"20240327\"}]}}";
        }

        try {
            return objectMapper.readValue(json, HyphenUserHouseListResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, "Mocked HyphenUserHouseListResponse 생성 중 오류가 발생했습니다.");
        }
    }

    private HyphenUserResidentRegistrationResponse getMockedHyphenUserResidentRegistrationResponse() {
        // 하이픈 응답값 세팅
        String json = "";
        try {
            return objectMapper.readValue(json, HyphenUserResidentRegistrationResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, "Mocked HyphenUserResidentRegistrationResponse 생성 중 오류가 발생했습니다.");
        }
    }

    // 주소분할 테스트 출력
    public Object addressParser(String address) {
        HouseAddressDto houseAddressDto = houseAddressService.parseAddress(address);

        log.debug(houseAddressDto.toString());

        Map<String, String> addressMap = new LinkedHashMap<>();
        addressMap.put("주소", houseAddressDto.formatAddress());
        addressMap.put("상세주소", houseAddressDto.getDetailAddress());
        addressMap.put("주택명", (houseAddressDto.formatEtcAddress().length() != 0 ? houseAddressDto.formatEtcAddress() : houseAddressDto.getSiDo() + "주택"));

        return ApiResponse.success(addressMap);
    }
}
