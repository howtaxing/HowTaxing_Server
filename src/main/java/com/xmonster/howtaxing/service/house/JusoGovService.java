package com.xmonster.howtaxing.service.house;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.jusogov.*;
import com.xmonster.howtaxing.feign.jusogov.JusoGovRoadAdrApi;
import com.xmonster.howtaxing.feign.jusogov.JusoGovRoadAdrDetailApi;
import com.xmonster.howtaxing.dto.jusogov.JusoGovAddrLinkApiResponse;
import com.xmonster.howtaxing.dto.jusogov.JusoGovAddrLinkApiResponse.Results.JusoDetail;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAddrListResponse;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAddrListResponse.Juso;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JusoGovService {
    private final JusoGovRoadAdrApi jusoGovRoadAdrApi;
    private final JusoGovRoadAdrDetailApi jusoGovRoadAdrDetailApi;

    private final static int COUNT_PER_PAGE = 5;
    private final static String DONG = "dong";
    private final static String FLOOR_HO = "floorho";

    @Value("${jusogov.confirm-key.road_addr}")
    private String confmKey_roadAddr;

    @Value("${jusogov.confirm-key.road_addr_detail}")
    private String confmKey_roadAddrDetail;

    // (취득)주택 도로명주소 조회
    public Object getHouseRoadAddrList(JusoGovRoadAddrListRequest jusoGovRoadAddrListRequest){
        log.info(">> [Service]JusoGovService getHouseRoadAddrList - (취득)주택 도로명주소 조회");

        // (취득)주택 도로명주소 조회 요청 값 유효성 체크
        validationCheckForGetHouseRoadAddrList(jusoGovRoadAddrListRequest);

        Integer currentPage = (jusoGovRoadAddrListRequest.getCurrentPage() != null) ? jusoGovRoadAddrListRequest.getCurrentPage() : 1;
        Integer countPerPage = (jusoGovRoadAddrListRequest.getCountPerPage() != null) ? jusoGovRoadAddrListRequest.getCountPerPage() : COUNT_PER_PAGE;
        String sido = StringUtils.defaultString(jusoGovRoadAddrListRequest.getSido());
        String sigungu = StringUtils.defaultString(jusoGovRoadAddrListRequest.getSigungu());
        String keyword = StringUtils.defaultString(jusoGovRoadAddrListRequest.getKeyword());
        StringBuilder fullKeyword = new StringBuilder(EMPTY);

        if(!EMPTY.equals(sido)){
            fullKeyword.append(sido);
        }

        if(!EMPTY.equals(sigungu)){
            fullKeyword.append(SPACE);
            fullKeyword.append(sigungu);
        }

        if(!EMPTY.contentEquals(fullKeyword)){
            fullKeyword.append(SPACE);
        }

        fullKeyword.append(keyword);

        log.info("keyword : " + fullKeyword.toString());

        ResponseEntity<?> response = jusoGovRoadAdrApi.getRoadAdrInfo(
                confmKey_roadAddr,
                currentPage,
                countPerPage,
                fullKeyword.toString(),
                JSON,
                YES,
                NONE,
                NO
        );

        if(response == null || response.getBody() == null){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공공기관에서 응답값을 받지 못했습니다.");
        }

        log.info(response.toString());

        JusoGovAddrLinkApiResponse jusoGovAddrLinkApiResponse = (JusoGovAddrLinkApiResponse) convertJsonToHouseData(response.getBody().toString(), 1);

        if(jusoGovAddrLinkApiResponse == null
                || jusoGovAddrLinkApiResponse.getResults() == null
                || jusoGovAddrLinkApiResponse.getResults().getCommon() == null
                || jusoGovAddrLinkApiResponse.getResults().getJuso() == null
                || jusoGovAddrLinkApiResponse.getResults().getCommon().getTotalCount() == null
                || jusoGovAddrLinkApiResponse.getResults().getCommon().getErrorCode() == null
        ){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공공기관에서 응답값을 받지 못했습니다.");
        }

        String errorCode = StringUtils.defaultString(jusoGovAddrLinkApiResponse.getResults().getCommon().getErrorCode());
        String errorMessage = StringUtils.defaultString(jusoGovAddrLinkApiResponse.getResults().getCommon().getErrorMessage());
        int totalCount = Integer.parseInt(jusoGovAddrLinkApiResponse.getResults().getCommon().getTotalCount());
        List<JusoDetail> jusoDetailList = jusoGovAddrLinkApiResponse.getResults().getJuso();
        List<Juso> jusoList = new ArrayList<>();

        if(!ZERO.equals(errorCode)){
            log.info("JusoGov errorMessage : " + errorMessage);
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "도로명주소 조회 중 오류가 발생했습니다.(공공기관 오류)");
        }

        if(totalCount > 0){
            for(JusoDetail jusoDetail : jusoDetailList){
                String roadAddr = StringUtils.defaultString(jusoDetail.getRoadAddr());
                String roadAddrPart1 = StringUtils.defaultString(jusoDetail.getRoadAddrPart1());
                String roadAddrPart2 = StringUtils.defaultString(jusoDetail.getRoadAddrPart2());
                String jibunAddr = StringUtils.defaultString(jusoDetail.getJibunAddr());
                String admCd = StringUtils.defaultString(jusoDetail.getAdmCd());
                String rnMgtSn = StringUtils.defaultString(jusoDetail.getRnMgtSn());
                String bdMgtSn = StringUtils.defaultString(jusoDetail.getBdMgtSn());
                String detBdNmList = StringUtils.defaultString(jusoDetail.getDetBdNmList());
                String bdNm = StringUtils.defaultString(jusoDetail.getBdNm());
                String bdKdcd = StringUtils.defaultString(jusoDetail.getBdKdcd());
                String udrtYn = StringUtils.defaultString(jusoDetail.getUdrtYn());
                String buldMnnm = StringUtils.defaultString(jusoDetail.getBuldMnnm());
                String buldSlno = StringUtils.defaultString(jusoDetail.getBuldSlno());
                String mtYn = StringUtils.defaultString(jusoDetail.getMtYn());

                String lnbrMnnm = StringUtils.defaultString(jusoDetail.getLnbrMnnm());
                String lnbrSlno = StringUtils.defaultString(jusoDetail.getLnbrSlno());
                lnbrMnnm = String.format("%4s", lnbrMnnm).replace(SPACE, ZERO);
                lnbrSlno = String.format("%4s", lnbrSlno).replace(SPACE, ZERO);

                //String pnu = EMPTY + admCd + ((ZERO.equals(mtYn)) ? ONE : TWO) + lnbrMnnm + lnbrSlno;
                String pnu = EMPTY;
                if(!EMPTY.equals(bdMgtSn) && bdMgtSn.length() == 25){
                    pnu = bdMgtSn.substring(0, 19);
                }

                // 건물명이 없으면 건물본번 - 건물부번으로 대체함
                if(EMPTY.equals(bdNm)){
                    bdNm = buldMnnm + HYPHEN + buldSlno;
                }

                jusoList.add(
                        Juso.builder()
                                .roadAddr(roadAddr)
                                .roadAddrPart1(roadAddrPart1)
                                .roadAddrPart2(roadAddrPart2)
                                .jibunAddr(jibunAddr)
                                .admCd(admCd)
                                .rnMgtSn(rnMgtSn)
                                .bdMgtSn(bdMgtSn)
                                .detBdNmList(detBdNmList)
                                .bdNm(bdNm)
                                .bdKdcd(bdKdcd)
                                .udrtYn(udrtYn)
                                .buldMnnm(buldMnnm)
                                .buldSlno(buldSlno)
                                .pnu(pnu)
                                .build());
            }
        }

        return ApiResponse.success(
                JusoGovRoadAddrListResponse.builder()
                        .totalCount(totalCount)
                        .currentPage(currentPage)
                        .countPerPage(countPerPage)
                        .jusoList(jusoList)
                        .build());
    }

    // (취득)주택 도로명주소 상세주소 조회
    public Object getHouseRoadAddrDetail(JusoGovRoadAddrDetailRequest jusoGovRoadAddrDetailRequest){
        log.info(">> [Service]JusoGovService getHouseRoadAddrDetail - (취득)주택 도로명주소 상세주소 조회");

        // (취득)주택 도로명주소 상세주소 조회 요청 값 유효성 체크
        validationCheckForGetHouseRoadAddrDetail(jusoGovRoadAddrDetailRequest);

        String searchType = jusoGovRoadAddrDetailRequest.getSearchType();
        String dongName = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getDongNm());
        if(!EMPTY.equals(dongName) && !dongName.endsWith("동")){
            dongName += "동";
        }

        ResponseEntity<?> response = jusoGovRoadAdrDetailApi.getRoadAdrDetailInfo(
                confmKey_roadAddrDetail,
                jusoGovRoadAddrDetailRequest.getAdmCd(),
                jusoGovRoadAddrDetailRequest.getRnMgtSn(),
                jusoGovRoadAddrDetailRequest.getUdrtYn(),
                jusoGovRoadAddrDetailRequest.getBuldMnnm(),
                jusoGovRoadAddrDetailRequest.getBuldSlno(),
                ONE.equals(searchType) ? DONG : FLOOR_HO,
                dongName,
                JSON
        );

        if(response == null || response.getBody() == null){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공공기관에서 응답값을 받지 못했습니다.");
        }

        log.info(response.toString());

        JusoGovAddrDetailApiResponse jusoGovAddrDetailApiResponse = (JusoGovAddrDetailApiResponse) convertJsonToHouseData(response.getBody().toString(), 2);

        if(jusoGovAddrDetailApiResponse == null
                || jusoGovAddrDetailApiResponse.getResults() == null
                || jusoGovAddrDetailApiResponse.getResults().getCommon() == null
                || jusoGovAddrDetailApiResponse.getResults().getJuso() == null
                || jusoGovAddrDetailApiResponse.getResults().getCommon().getTotalCount() == null
                || jusoGovAddrDetailApiResponse.getResults().getCommon().getErrorCode() == null
        ){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공공기관에서 응답값을 받지 못했습니다.");
        }

        String errorCode = StringUtils.defaultString(jusoGovAddrDetailApiResponse.getResults().getCommon().getErrorCode());
        String errorMessage = StringUtils.defaultString(jusoGovAddrDetailApiResponse.getResults().getCommon().getErrorMessage());
        int totalCount = Integer.parseInt(jusoGovAddrDetailApiResponse.getResults().getCommon().getTotalCount());
        List<String> dongHoList = new ArrayList<>();

        if(!ZERO.equals(errorCode)){
            log.info("JusoGov errorMessage : " + errorMessage);
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "도로명주소 상세주소 조회 중 오류가 발생했습니다.(공공기관 오류)");
        }

        for(int i=0; i<totalCount; i++){
            // 동 조회
            if(ONE.equals(searchType)){
                String dongNm = StringUtils.defaultString(jusoGovAddrDetailApiResponse.getResults().getJuso().get(i).getDongNm());

                if(dongNm.endsWith("동")){
                    dongHoList.add(dongNm.replace("동", EMPTY));
                }
            }else{
                String hoNm = StringUtils.defaultString(jusoGovAddrDetailApiResponse.getResults().getJuso().get(i).getHoNm());

                if(hoNm.endsWith("호")){
                    dongHoList.add(hoNm.replace("호", EMPTY));
                }
            }
        }

        return ApiResponse.success(
                JusoGovRoadAddrDetailResponse.builder()
                        .totalCount(totalCount)
                        .searchType(searchType)
                        .dongHoList(dongHoList)
                        .build());
    }

    // 변경 예정
    public JusoGovRoadAdrResponse getRoadAdrInfo(String keyword){

        ResponseEntity<?> response = jusoGovRoadAdrApi.getRoadAdrInfo(
                confmKey_roadAddr,
                1,
                10,
                keyword,
                JSON,
                YES,
                NONE,
                NO
        );

        log.info("jusogov road address response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null) jsonString = response.getBody().toString();

        return (JusoGovRoadAdrResponse) convertJsonToHouseData(jsonString, 3);
    }

    // (취득)주택 도로명주소 조회 요청 값 유효성 체크
    private void validationCheckForGetHouseRoadAddrList(JusoGovRoadAddrListRequest jusoGovRoadAddrListRequest){
        log.info(">>> JusoGovService validationCheckForGetHouseRoadAddrList - (취득)주택 도로명주소 조회 요청 값 유효성 체크");

        if(jusoGovRoadAddrListRequest == null) throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 요청 값이 입력되지 않았습니다.");

        String sido = StringUtils.defaultString(jusoGovRoadAddrListRequest.getSido());
        String sigungu = StringUtils.defaultString(jusoGovRoadAddrListRequest.getSigungu());
        String keyword = StringUtils.defaultString(jusoGovRoadAddrListRequest.getKeyword());

        if(EMPTY.equals(sido) || EMPTY.equals(sigungu)){
            if(EMPTY.equals(keyword)){
                //throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 검색어가 입력되지 않았습니다.");
                throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위해 시/도 및 시/군/구를 모두 입력하거나, 검색어를 입력해주세요.");
            }
        }
    }

    // (취득)주택 도로명주소 상세주소 조회 요청 값 유효성 체크
    private void validationCheckForGetHouseRoadAddrDetail(JusoGovRoadAddrDetailRequest jusoGovRoadAddrDetailRequest){
        log.info(">>> JusoGovService validationCheckForGetHouseRoadAddrList - (취득)주택 도로명주소 상세주소 조회 요청 값 유효성 체크");

        if(jusoGovRoadAddrDetailRequest == null) throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 상세주소 조회를 위한 요청 값이 입력되지 않았습니다.");

        String admCd = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getAdmCd());
        String rnMgtSn = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getRnMgtSn());
        String udrtYn = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getUdrtYn());
        String buldMnnm = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getBuldMnnm());
        String buldSlno = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getBuldSlno());
        String searchType = StringUtils.defaultString(jusoGovRoadAddrDetailRequest.getSearchType());

        if(EMPTY.equals(admCd)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 행정구역코드가 입력되지 않았습니다.");
        }

        if(EMPTY.equals(rnMgtSn)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 도로명코드가 입력되지 않았습니다.");
        }

        if(EMPTY.equals(udrtYn)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 지하여부(0:지상, 1:지하) 값이 입력되지 않았습니다.");
        }

        if(EMPTY.equals(buldMnnm)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 건물본번이 입력되지 않았습니다.");
        }

        if(EMPTY.equals(buldSlno)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 건물부번이 입력되지 않았습니다.");
        }

        if(EMPTY.equals(searchType)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 검색유형이 입력되지 않았습니다.");
        }

        if(!ONE.equals(searchType) && !TWO.equals(searchType)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "도로명주소 조회를 위한 검색유형이 올바르지 않습니다.");
        }
    }

    private static Object convertJsonToHouseData(String jsonString, int type) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            if(type == 1){
                return objectMapper.readValue(jsonString, JusoGovAddrLinkApiResponse.class);
            }else if(type == 2){
                return objectMapper.readValue(jsonString, JusoGovAddrDetailApiResponse.class);
            }else{
                return objectMapper.readValue(jsonString, JusoGovRoadAdrResponse.class);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR);
        }
    }
}