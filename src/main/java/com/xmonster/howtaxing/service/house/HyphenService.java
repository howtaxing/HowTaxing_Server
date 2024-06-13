package com.xmonster.howtaxing.service.house;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.house.HouseListSearchRequest;
import com.xmonster.howtaxing.dto.house.HouseStayPeriodRequest;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationResponse.HyphenUserResidentRegistrationCommon;
import com.xmonster.howtaxing.dto.hyphen.*;
import com.xmonster.howtaxing.feign.hyphen.HyphenAuthApi;
import com.xmonster.howtaxing.feign.hyphen.HyphenUserOwnHouseApi;

import com.xmonster.howtaxing.feign.hyphen.HyphenUserResidentRegistrationApi;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HyphenService {
    private final HyphenAuthApi hyphenAuthApi;
    private final HyphenUserOwnHouseApi hyphenUserOwnHouseApi;
    private final HyphenUserResidentRegistrationApi hyphenUserResidentRegistrationApi;

    @Value("${hyphen.user_id}")
    private String userId;
    @Value("${hyphen.hkey}")
    private String hKey;

    public Optional<HyphenAuthResponse> getAccessToken(){
        ResponseEntity<?> response = hyphenAuthApi.getAccessToken(
            HyphenRequestAccessTokenDto.builder()
                .user_id(userId)
                .hkey(hKey)
                .build()
        );

        log.info("hyphen auth info");
        log.info(response.toString());

        return Optional.ofNullable(new Gson()
                .fromJson(
                        response.getBody().toString(),
                        HyphenAuthResponse.class
                )
        );
    }

    public Optional<HyphenUserHouseListResponse> getUserHouseInfo(String accessToken, HouseListSearchRequest houseListSearchRequest){
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("authorization", "Bearer " + accessToken);

        if(houseListSearchRequest == null) throw new CustomException(ErrorCode.HOUSE_HYPHEN_INPUT_ERROR);

        ResponseEntity<?> response = hyphenUserOwnHouseApi.getUserOwnHouseInfo(
            headerMap,
            HyphenUserHouseListRequest.builder()
                    .loginMethod(EASY)
                    .loginOrgCd(houseListSearchRequest.getCertOrg())
                    .bizNo(houseListSearchRequest.getRlno())
                    .userId(houseListSearchRequest.getUserId())
                    .userPw(houseListSearchRequest.getUserPw())
                    .mobileNo(houseListSearchRequest.getMobileNo())
                    .userNm(houseListSearchRequest.getUserNm())
                    .build()
        );

        log.info("hyphen user house response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null)  jsonString = response.getBody().toString();
        System.out.println("jsonString : " + jsonString);

        //HyphenUserHouseListResponse hyphenUserHouseListResponse = convertJsonToHouseData(jsonString);
        HyphenUserHouseListResponse hyphenUserHouseListResponse = (HyphenUserHouseListResponse) convertJsonToData(jsonString, 1);
        System.out.println("hyphenUserHouseListResponse : " + hyphenUserHouseListResponse);

        return Optional.ofNullable(hyphenUserHouseListResponse);
    }

    public Optional<HyphenUserResidentRegistrationResponse> getUserStayPeriodInfo(HouseStayPeriodRequest houseStayPeriodRequest){
        Map<String, Object> headerMap = new HashMap<>();
        // TODO. headerMap 추가 필요

        validationCheckForGetUserStayPeriodInfo(houseStayPeriodRequest);

        String step = (ONE.equals(houseStayPeriodRequest.getStep())) ? "init" : "sign";

        ResponseEntity<?> response = hyphenUserResidentRegistrationApi.getUserResidentRegistrationInfo(
                headerMap,
                HyphenUserResidentRegistrationRequest.builder()
                        .sido(houseStayPeriodRequest.getSido())
                        .sigg(houseStayPeriodRequest.getSigungu())
                        .cusGb(INDVD_LOCAL)
                        .userName(houseStayPeriodRequest.getUserName())
                        .bizNo(houseStayPeriodRequest.getRlno())
                        .req2Opt1(NOT_INCLUDE)
                        .req2Opt2(INCLUDE)
                        .req2Opt3(INCLUDE)
                        .req2Opt4(NOT_INCLUDE)
                        .req2Opt5(NOT_INCLUDE)
                        .req2Opt6(NOT_INCLUDE)
                        .req2Opt7(NOT_INCLUDE)
                        .req2Opt8(NOT_INCLUDE)
                        .authMethod(EASY)
                        .loginOrgCd(houseStayPeriodRequest.getLoginOrgCd())
                        .mobileNo(houseStayPeriodRequest.getMobileNo())
                        .mobileCo(houseStayPeriodRequest.getMobileCo())
                        .step(houseStayPeriodRequest.getStep())
                        .stepData(houseStayPeriodRequest.getStepData())
                        .build()
        );

        log.info("hyphen user stay period response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null)  jsonString = response.getBody().toString();
        System.out.println("jsonString : " + jsonString);

        HyphenUserResidentRegistrationResponse hyphenUserResidentRegistrationResponse = (HyphenUserResidentRegistrationResponse) convertJsonToData(jsonString, 2);
        System.out.println("hyphenUserResidentRegistrationResponse : " + hyphenUserResidentRegistrationResponse);

        if(hyphenUserResidentRegistrationResponse == null
                || hyphenUserResidentRegistrationResponse.getHyphenUserResidentRegistrationCommon() == null
                || hyphenUserResidentRegistrationResponse.getHyphenUserResidentRegistrationCommon().getErrYn() == null
                || hyphenUserResidentRegistrationResponse.getHyphenUserResidentRegistrationData() == null
        ){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR, "공공기관에서 응답값을 받지 못했습니다.");
        }

        if(YES.equals(hyphenUserResidentRegistrationResponse.getHyphenUserResidentRegistrationCommon().getErrYn())){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR, "주택 거주기간 조회 중 오류가 발생했습니다.(공공기관 오류)");
        }

        return Optional.of(hyphenUserResidentRegistrationResponse);
    }

    private void validationCheckForGetUserStayPeriodInfo(HouseStayPeriodRequest houseStayPeriodRequest){
        if(houseStayPeriodRequest == null) throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR);

        String userName = StringUtils.defaultString(houseStayPeriodRequest.getUserName());
        String mobileNo = StringUtils.defaultString(houseStayPeriodRequest.getMobileNo());
        String rlno = StringUtils.defaultString(houseStayPeriodRequest.getRlno());
        String loginOrgCd = StringUtils.defaultString(houseStayPeriodRequest.getLoginOrgCd());
        String mobileCo = StringUtils.defaultString(houseStayPeriodRequest.getMobileCo());
        String sido = StringUtils.defaultString(houseStayPeriodRequest.getSido());
        String sigungu = StringUtils.defaultString(houseStayPeriodRequest.getSigungu());
        String step = StringUtils.defaultString(houseStayPeriodRequest.getStep());
        String stepData = StringUtils.defaultString(houseStayPeriodRequest.getStepData());

        if(EMPTY.equals(userName)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 이름이 입력되지 않았습니다.");
        }

        if(EMPTY.equals(mobileNo)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 휴대폰번호가 입력되지 않았습니다.");
        }

        if(EMPTY.equals(rlno)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 주민등록번호가 입력되지 않았습니다.");
        }

        if(EMPTY.equals(loginOrgCd)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 간편로그인 기관구분이 입력되지 않았습니다.");
        }

        if(!PASS.equals(loginOrgCd) && !KAKAO.equals(loginOrgCd) && !PAYCO.equals(loginOrgCd) && !KICA.equals(loginOrgCd) && !KB.equals(loginOrgCd)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 간편로그인 기관구분이 올바르게 입력되지 않았습니다.(PASS인증:pass, 카카오톡:kakao, 페이코:payco, 삼성패스:kica, KB스타뱅킹:kb)");
        }

        if(PASS.equals(loginOrgCd)){
            if(EMPTY.equals(mobileCo)){
                throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 통신사 정보가 입력되지 않았습니다.(간편로그인 기관구분이 PASS인 경우 필수)");
            }

            if(!SKT.equals(mobileCo) && !KT.equals(mobileCo) && !LGU.equals(mobileCo)){
                throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 통신사 정보가 올바르게 입력되지 않았습니다.(SKT:S, KT:K, LGU+:L *앋뜰통신사구분없음)");
            }
        }

        if(EMPTY.equals(sido)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 시도 값이 입력되지 않았습니다.");
        }

        if(EMPTY.equals(sigungu)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 시군구 값이 입력되지 않았습니다.");
        }

        if(EMPTY.equals(step)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 STEP 값이 입력되지 않았습니다.");
        }

        if(!INIT.equals(step) && !SIGN.equals(step)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 STEP 값이 올바르게 입력되지 않았습니다.(init:1, sign:2)");
        }

        if(SIGN.equals(step) && EMPTY.equals(stepData)){
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR, "주택 거주기간 조회를 위한 STEPDATA 값이 입력되지 않았습니다.");
        }
    }

    private Object convertJsonToData(String jsonString, int type) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            if(type == 1){
                return objectMapper.readValue(jsonString, HyphenUserHouseListResponse.class);
            }else if(type == 2){
                return objectMapper.readValue(jsonString, HyphenUserResidentRegistrationResponse.class);
            }else{
                return objectMapper.readValue(jsonString, HyphenUserHouseListResponse.class);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            if(type == 1){
                throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR);
            }else if(type == 2){
                throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR);
            }else{
                throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR);
            }
        }
    }
}
