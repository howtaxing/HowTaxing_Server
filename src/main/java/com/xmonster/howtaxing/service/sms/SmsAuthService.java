package com.xmonster.howtaxing.service.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.sms.*;
import com.xmonster.howtaxing.dto.sms.SmsMessageBodyRequest.Message;
import com.xmonster.howtaxing.feign.sms.SmsSendApi;
import com.xmonster.howtaxing.model.SmsAuthInfo;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.sms.SmsAuthRepository;
import com.xmonster.howtaxing.type.*;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SmsAuthService {
    private final SmsSendApi smsSendApi;
    private final SmsAuthRepository smsAuthRepository;
    private final UserUtil userUtil;

    private static final String SMS_STATUS_CODE_SUCCESS = "202";    // SMS 발송 성공(Naver Cloud Platform 응답값)

    @Value("${ncloud.access-key}")
    private String accessKey;

    @Value("${ncloud.secret-key}")
    private String secretKey;

    @Value("${ncloud.sms.service-id}")
    private String serviceId;

    @Value("${ncloud.sms.sender-number}")
    private String senderNumber;

    // 인증번호 발송
    public Object sendAuthCode(SmsSendAuthCodeRequest smsSendAuthCodeRequest) throws Exception {
        log.info(">> [Service]SmsAuthService sendAuthCode - 인증번호 발송");

        // 인증번호 발송 유효값 체크
        this.validationCheckForSendAuthCode(smsSendAuthCodeRequest);

        String phoneNumber = smsSendAuthCodeRequest.getPhoneNumber().replace(HYPHEN, EMPTY);
        AuthType authType = AuthType.valueOf(smsSendAuthCodeRequest.getAuthType());
        String id = smsSendAuthCodeRequest.getId();

        // 아이디 찾기
        if(AuthType.FIND_ID.equals(authType)){
            User findUser = userUtil.findUserByPhoneNumber(phoneNumber);

            if(findUser == null || StringUtils.isBlank(findUser.getSocialId())){
                throw new CustomException(ErrorCode.ID_FIND_PHONE_ERROR);   // 입력한 휴대폰 번호로 가입된 아이디를 찾을 수 없어요.
            }

            SocialType socialType = findUser.getSocialType();

            if(SocialType.KAKAO.equals(socialType)){
                throw new CustomException(ErrorCode.ID_FIND_KAKAO_ERROR);   // 회원님은 카카오로 가입되어 있어 아이디 찾기가 불가해요.
            }else if(SocialType.NAVER.equals(socialType)){
                throw new CustomException(ErrorCode.ID_FIND_NAVER_ERROR);   // 회원님은 네이버로 가입되어 있어 아이디 찾기가 불가해요.
            }
        }
        // 비밀번호 재설정
        else if(AuthType.RESET_PW.equals(authType)){
            User findUser = userUtil.findUserBySocialId(id);

            if(findUser == null){
                throw new CustomException(ErrorCode.PW_RESET_ID_ERROR);     // 가입된 아이디가 아니에요.
            }

            if(!phoneNumber.equals(findUser.getPhoneNumber())){
                throw new CustomException(ErrorCode.PW_RESET_PHONE_ERROR);  // 입력한 아이디에 등록된 휴대폰 번호가 아니에요.
            }

            SocialType socialType = findUser.getSocialType();

            if(SocialType.KAKAO.equals(socialType)){
                throw new CustomException(ErrorCode.PW_RESET_KAKAO_ERROR);   // 회원님은 카카오로 가입되어 있어 비밀번호 재설정이 불가해요.
            }else if(SocialType.NAVER.equals(socialType)){
                throw new CustomException(ErrorCode.PW_RESET_NAVER_ERROR);   // 회원님은 네이버로 가입되어 있어 비밀번호 재설정이 불가해요.
            }
        }
        // 회원 가입
        else if(AuthType.JOIN.equals(authType)){
            User findUser = userUtil.findUserBySocialId(id);

            if(findUser != null && Role.USER.equals(findUser.getRole())){
                SocialType socialType = findUser.getSocialType();

                if(SocialType.KAKAO.equals(socialType)){
                    throw new CustomException(ErrorCode.JOIN_DUPLICATE_KAKAO_ERROR);   // 해당 휴대폰 번호는 이미 카카오를 통해 가입된 계정에서 사용 중이에요.
                }else if(SocialType.NAVER.equals(socialType)){
                    throw new CustomException(ErrorCode.JOIN_DUPLICATE_NAVER_ERROR);   // 해당 휴대폰 번호는 이미 네이버를 통해 가입된 계정에서 사용 중이에요.
                }else if(SocialType.IDPASS.equals(socialType)){
                    throw new CustomException(ErrorCode.JOIN_DUPLICATE_IDPASS_ERROR);   // 해당 휴대폰 번호는 이미 가입된 계정에서 사용 중이에요.
                }
            }
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todaySendCount = smsAuthRepository.countByPhoneNumberAndAuthTypeAndSendDatetimeBetween(phoneNumber, authType, startOfDay, endOfDay);

        // 인증유형 별 인증번호 10회 이상 발송 불가
        if(todaySendCount > 10) throw new CustomException(ErrorCode.SMS_AUTH_COUNT_ERROR);

        // 인증번호 생성
        String authCode = generateAuthCode();

        // 인증번호 발송(Naver Cloud Platform)
        SmsMessageBodyResponse smsMessageBodyResponse = this.sendSmsByNaverCloudPlatform(phoneNumber, authCode);

        // 인증번호 발송 실패
        if(smsMessageBodyResponse == null || !SMS_STATUS_CODE_SUCCESS.equals(smsMessageBodyResponse.getStatusCode())){
            throw new CustomException(ErrorCode.SMS_AUTH_SEND_ERROR);
        }

        LocalDateTime sendDatetime = LocalDateTime.now();

        // 인증정보 저장
        smsAuthRepository.saveAndFlush(
                SmsAuthInfo.builder()
                        .phoneNumber(phoneNumber)
                        .socialId(id)
                        .authType(authType)
                        .authCode(authCode)
                        .sendDatetime(sendDatetime)
                        .isAuthKeyUsed(false)
                        .build());

        //return ApiResponse.success(Map.of("result", "입력하신 휴대폰 번호로 인증번호를 발송했어요."));
        return ApiResponse.success(
                SmsSendAuthCodeResponse.builder()
                        .phoneNumber(phoneNumber)
                        .id(id)
                        .authType(authType.toString())
                        .sendDatetime(sendDatetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build());
    }

    // 인증번호 검증
    public Object checkAuthCode(SmsCheckAuthCodeRequest smsCheckAuthCodeRequest) throws Exception {
        log.info(">> [Service]SmsAuthService checkAuthCode - 인증번호 검증");

        // 인증번호 검증 유효값 체크
        this.validationCheckForCheckAuthCode(smsCheckAuthCodeRequest);

        String phoneNumber = smsCheckAuthCodeRequest.getPhoneNumber().replace(HYPHEN, EMPTY);
        AuthType authType = AuthType.valueOf(smsCheckAuthCodeRequest.getAuthType());
        String authCode = smsCheckAuthCodeRequest.getAuthCode();
        
        SmsAuthInfo smsAuthInfo = smsAuthRepository.findTopByPhoneNumberAndAuthTypeOrderBySendDatetimeDesc(phoneNumber, authType);
        if(smsAuthInfo == null){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "발송된 인증번호가 없습니다.");
        }

        LocalDateTime sendDatetime = smsAuthInfo.getSendDatetime();
        String orgAuthCode = StringUtils.defaultString(smsAuthInfo.getAuthCode());

        // 인증시간 만료(3분)
        if(LocalDateTime.now().minusMinutes(3).isAfter(sendDatetime)){
            throw new CustomException(ErrorCode.SMS_AUTH_TIME_ERROR);
        }

        // 인증번호 불일치
        if(!orgAuthCode.equals(authCode)){
            throw new CustomException(ErrorCode.SMS_AUTH_MATCH_ERROR);
        }

        LocalDateTime authDatetime = LocalDateTime.now();   // 인증일시
        String authKey = this.generateAuthKey();            // 인증키(생성)

        // 인증일시 및 인증키를 SMS 인증정보에 저장
        smsAuthInfo.setAuthDatetime(authDatetime);
        smsAuthInfo.setAuthKey(authKey);
        smsAuthRepository.save(smsAuthInfo);

        //return ApiResponse.success(Map.of("result", "인증번호가 일치합니다."));
        return ApiResponse.success(
                SmsCheckAuthCodeResponse.builder()
                        .phoneNumber(phoneNumber)
                        .id(smsAuthInfo.getSocialId())
                        .authType(authType.toString())
                        .sendDatetime(sendDatetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .authDatetime(authDatetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .authKey(authKey)
                        .build());
    }

    // 인증키 검증
    public boolean checkAuthKey(String authKey) throws Exception {
        log.info(">> [Service]SmsAuthService checkAuthKey - 인증키 검증");

        if(StringUtils.isBlank(authKey)){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "인증키가 입력되지 않았습니다.");
        }

        log.info("authKey : " + authKey);

        if(authKey.length() != 30){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "정확한 인증키를 입력해주세요.");
        }

        SmsAuthInfo smsAuthInfo = smsAuthRepository.findTopByAuthKeyOrderByAuthDatetimeDesc(authKey);
        if(smsAuthInfo == null){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "해당 인증키가 없습니다.");
        }

        String orgAuthKey = StringUtils.defaultString(smsAuthInfo.getAuthKey());
        LocalDateTime authDatetime = smsAuthInfo.getAuthDatetime();
        boolean isAuthKeyUsed = smsAuthInfo.getIsAuthKeyUsed();
        
        return orgAuthKey.equals(authKey) && !isAuthKeyUsed && LocalDateTime.now().minusDays(1).isBefore(authDatetime);
    }

    // 인증키 사용 완료 세팅
    public void setAuthKeyUsed(String authKey){
        log.info(">> [Service]SmsAuthService setAuthKeyUsed - 인증키 사용 완료 세팅");

        if(StringUtils.isBlank(authKey)){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "인증키가 입력되지 않았습니다.");
        }

        log.info("authKey : " + authKey);

        if(authKey.length() != 30){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "정확한 인증키를 입력해주세요.");
        }

        SmsAuthInfo smsAuthInfo = smsAuthRepository.findTopByAuthKeyOrderByAuthDatetimeDesc(authKey);
        if(smsAuthInfo == null){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "해당 인증키가 없습니다.");
        }

        smsAuthInfo.setIsAuthKeyUsed(true);
        smsAuthRepository.save(smsAuthInfo);
    }

    // 인증번호 발송 유효값 체크
    private void validationCheckForSendAuthCode(SmsSendAuthCodeRequest smsSendAuthCodeRequest){
        if(smsSendAuthCodeRequest == null){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR);
        }

        log.info(smsSendAuthCodeRequest.toString());

        String phoneNumber = smsSendAuthCodeRequest.getPhoneNumber();
        String id = smsSendAuthCodeRequest.getId();
        String authType = smsSendAuthCodeRequest.getAuthType();

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }
        
        if(StringUtils.isBlank(authType)){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "인증유형이 입력되지 않았습니다.");
        }else{
            if(!AuthType.JOIN.toString().equals(authType) &&
                    !AuthType.FIND_ID.toString().equals(authType) &&
                    !AuthType.RESET_PW.toString().equals(authType) &&
                    !AuthType.ETC.toString().equals(authType)){
                throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "정확한 인증 유형을 입력해주세요.(JOIN:회원가입, FIND_ID:아이디찾기, RESET_PW:비밀번호재설정)");
            }

            // 아이디 찾기를 제외한 다른 케이스에는 아이디 입력여부 체크
            if(!AuthType.FIND_ID.toString().equals(authType)){
                if(StringUtils.isBlank(id)){
                    throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "아이디가 입력되지 않았습니다.");
                }
            }
        }
    }

    // 인증번호 검증 유효값 체크
    private void validationCheckForCheckAuthCode(SmsCheckAuthCodeRequest smsCheckAuthCodeRequest){
        if(smsCheckAuthCodeRequest == null){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "인증번호 검증을 위한 입력값이 올바르지 않아요.");
        }

        log.info(smsCheckAuthCodeRequest.toString());

        String phoneNumber = smsCheckAuthCodeRequest.getPhoneNumber();
        String authType = smsCheckAuthCodeRequest.getAuthType();

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }

        if(StringUtils.isBlank(authType)){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "인증유형이 입력되지 않았습니다.");
        }else{
            if(!AuthType.JOIN.toString().equals(authType) &&
                    !AuthType.FIND_ID.toString().equals(authType) &&
                    !AuthType.RESET_PW.toString().equals(authType) &&
                    !AuthType.ETC.toString().equals(authType)){
                throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "정확한 인증 유형을 입력해주세요.(JOIN:회원가입, FIND_ID:아이디찾기, RESET_PW:비밀번호재설정)");
            }
        }
    }

    // 네이버 클라우드 플랫폼을 통한 SMS 발송
    private SmsMessageBodyResponse sendSmsByNaverCloudPlatform(String phoneNumber, String authCode) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        Long time = System.currentTimeMillis();
        Map<String, Object> headerMap = new HashMap<>();

        headerMap.put("x-ncp-apigw-timestamp", time.toString());
        headerMap.put("x-ncp-iam-access-key", this.accessKey);
        headerMap.put("x-ncp-apigw-signature-v2", this.makeSignature(time));

        String messageContent = "[하우택싱] 인증번호는 [" + authCode + "] 입니다.";

        ResponseEntity<?> response = null;
        try{
            List<Message> messageList = new ArrayList<>();
            messageList.add(
                    Message.builder()
                            .to(phoneNumber)
                            .content(messageContent)
                            .build());

            response = smsSendApi.sendSms(
                    headerMap,
                    SmsMessageBodyRequest.builder()
                            .type("SMS")
                            .contentType("COMM")
                            .from(senderNumber)
                            .content("하우택싱 기본 메시지 내용")
                            .messages(messageList)
                            .build());
        }catch(Exception e){
            log.error("SMS 발송 오류 내용 : " + e.getMessage());
            throw new CustomException(ErrorCode.SMS_AUTH_SEND_ERROR);
        }

        log.info("sms send response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null)  jsonString = response.getBody().toString();
        System.out.println("jsonString : " + jsonString);

        SmsMessageBodyResponse smsMessageBodyResponse = (SmsMessageBodyResponse) convertJsonToData(jsonString);
        System.out.println("smsMessageBodyResponse : " + smsMessageBodyResponse);

        return smsMessageBodyResponse;
    }

    // 인증키 생성
    private String makeSignature(Long time) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String space = " ";					// one space
        String newLine = "\n";					// new line
        String method = "POST";					// method
        String url = "/sms/v2/services/" + this.serviceId + "/messages";	// url (include query string)
        String timestamp = time.toString();			// current timestamp (epoch)
        String accessKey = this.accessKey;			// access key id (from portal or Sub Account)
        String secretKey = this.secretKey;

        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeBase64String(rawHmac);
    }

    // 인증번호 생성
    private String generateAuthCode(){
        final String chars = "0123456789";
        final int codeLength = 6;
        SecureRandom sRandom = new SecureRandom();

        StringBuilder code = new StringBuilder(codeLength);

        for(int i=0; i<codeLength; i++){
            code.append(chars.charAt(sRandom.nextInt(chars.length())));
        }

        return code.toString();
    }

    // 인증키 생성
    private String generateAuthKey(){
        final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final int keyLength = 30;
        SecureRandom sRandom = new SecureRandom();

        StringBuilder key = new StringBuilder(keyLength);

        for(int i=0; i<keyLength; i++){
            key.append(chars.charAt(sRandom.nextInt(chars.length())));
        }

        return key.toString();
    }

    // 데이터 변환(Json -> Object)
    private Object convertJsonToData(String jsonString) {
        String errMsgDtl = null;

        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, SmsMessageBodyResponse.class);
        }catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.SMS_AUTH_SEND_ERROR);
        }
    }
}
