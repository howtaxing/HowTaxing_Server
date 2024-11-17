package com.xmonster.howtaxing.service.house;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.house.HouseListSearchRequest;
import com.xmonster.howtaxing.dto.house.HouseStayPeriodRequest;
import com.xmonster.howtaxing.dto.hyphen.HyphenCommonResponse;
import com.xmonster.howtaxing.dto.hyphen.HyphenCommonResponse.HyphenCommon;
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

import java.net.SocketTimeoutException;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HyphenService {
    private final HyphenAuthApi hyphenAuthApi;
    private final HyphenUserOwnHouseApi hyphenUserOwnHouseApi;
    private final HyphenUserResidentRegistrationApi hyphenUserResidentRegistrationApi;

    private final static String LOGIN_109 = "[LOGIN-109]";  // 청약통장이 없거나 주민등록번호가 틀린 경우 오류 코드
    private final static String LOGIN_999 = "[LOGIN-999]";  // 아이디/패스워드가 틀렸거나 해당 간편인증 수단의 회원이 아닌 경우 오류코드

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

    public Optional<HyphenUserHouseListResponse> getUserHouseInfo(HouseListSearchRequest houseListSearchRequest){
        Map<String, Object> headerMap = new HashMap<>();
        HyphenAuthResponse hyphenAuthResponse = getAccessToken()
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, "하이픈에서 AccessToken을 가져오는데 실패했습니다."));

        headerMap.put("authorization", "Bearer " + hyphenAuthResponse.getAccess_token());

        if(houseListSearchRequest == null) throw new CustomException(ErrorCode.HOUSE_HYPHEN_INPUT_ERROR);

        ResponseEntity<?> response = null;
        try{
            response = hyphenUserOwnHouseApi.getUserOwnHouseInfo(
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
        }catch(Exception e){
            log.error("보유주택조회 간편인증 오류 내용 : " + e.getMessage());
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_SYSTEM_ERROR, "청약홈 간편인증 중 오류가 발생했습니다.(청약통장을 보유하고 있지 않거나 인증앱에 문제가 있는 경우 오류가 발생할 수 있습니다.)");
        }

        log.info("hyphen user house response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null)  jsonString = response.getBody().toString();
        System.out.println("jsonString : " + jsonString);

        HyphenUserHouseListResponse hyphenUserHouseListResponse = (HyphenUserHouseListResponse) convertJsonToData(jsonString, 1);
        System.out.println("hyphenUserHouseListResponse : " + hyphenUserHouseListResponse);

        return Optional.ofNullable(hyphenUserHouseListResponse);
    }

    public Optional<HyphenUserResidentRegistrationResponse> getUserStayPeriodInfo(HouseStayPeriodRequest houseStayPeriodRequest){
        Map<String, Object> headerMap = new HashMap<>();
        HyphenAuthResponse hyphenAuthResponse = getAccessToken()
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, "하이픈에서 AccessToken을 가져오는데 실패했습니다."));

        headerMap.put("authorization", "Bearer " + hyphenAuthResponse.getAccess_token());

        validationCheckForGetUserStayPeriodInfo(houseStayPeriodRequest);

        String step = (ONE.equals(houseStayPeriodRequest.getStep())) ? "init" : "sign";

        ResponseEntity<?> response = null;

        try{
            response = hyphenUserResidentRegistrationApi.getUserResidentRegistrationInfo(
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
                            .req2Opt5(INCLUDE)
                            .req2Opt6(INCLUDE)
                            .req2Opt7(NOT_INCLUDE)
                            .req2Opt8(NOT_INCLUDE)
                            .authMethod(EASY)
                            .loginOrgCd(houseStayPeriodRequest.getLoginOrgCd())
                            .mobileNo(houseStayPeriodRequest.getMobileNo())
                            .mobileCo(houseStayPeriodRequest.getMobileCo())
                            .step(step)
                            .stepData(houseStayPeriodRequest.getStepData())
                            .build()
            );
        }catch(Exception e){
            log.error("주민등록초본 간편인증 오류 내용 : " + e.getMessage());
            throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR);
        }

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
        if(houseStayPeriodRequest == null) throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_INPUT_ERROR);

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
        String errMsgDtl = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            HyphenCommonResponse hyphenCommonResponse = objectMapper.readValue(jsonString, HyphenCommonResponse.class);

            if(hyphenCommonResponse != null && hyphenCommonResponse.getHyphenCommon() != null){
                if(YES.equals(hyphenCommonResponse.getHyphenCommon().getErrYn())){
                    errMsgDtl = hyphenCommonResponse.getHyphenCommon().getErrMsg();
                    log.info("errMsgDtl : " + errMsgDtl);
                    throw new CustomException(ErrorCode.ETC_ERROR); // 하단 catch로 이동
                }
            }

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
                //errMsgDtl = changeErrDtlContents(errMsgDtl);
                if(StringUtils.isNotBlank(errMsgDtl)){
                    if(errMsgDtl.contains(LOGIN_109)){
                        // 청약통장이 없거나 주민등록번호가 잘못 입력되어 청약홈 인증에 실패하였습니다.
                        throw new CustomException(ErrorCode.HOUSE_HYPHEN_RLNO_ERROR);
                    }else if(errMsgDtl.contains(LOGIN_999)){
                        // 해당 간편인증 회원이 아니거나 아이디 또는 패스워드가 틀려 청약홈 인증에 실패했습니다.
                        throw new CustomException(ErrorCode.HOUSE_HYPHEN_ACCOUNT_ERROR);
                    }else{
                        throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, errMsgDtl);
                    }
                }else{
                    throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, errMsgDtl);
                }
            }else if(type == 2){
                throw new CustomException(ErrorCode.HYPHEN_STAY_PERIOD_OUTPUT_ERROR, errMsgDtl);
            }else{
                throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR, errMsgDtl);
            }
        }
    }

    /*private String changeErrDtlContents(String errMsgDtl){
        String resultErrDtl = StringUtils.defaultString(errMsgDtl);

        // 청약통장이 없거나 주민등록번호가 틀린 경우
        if(resultErrDtl.contains(LOGIN_109)){
            resultErrDtl = "청약통장이 없거나 주민등록번호가 잘못 입력되어 청약홈 인증에 실패하였습니다.";
        }
        // 아이디/패스워드가 틀렸거나 해당 간편인증 수단의 회원이 아닌 경우
        else if(resultErrDtl.contains(LOGIN_999)){
            resultErrDtl = "선택하신 간편인증 서비스의 회원이 아니거나 입력하신 아이디 또는 패스워드가 맞지 않아 청약홈 인증에 실패했습니다.";
        }

        return resultErrDtl;
    }*/
}

