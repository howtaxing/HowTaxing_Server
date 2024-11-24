package com.xmonster.howtaxing.service.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.sms.SmsCheckAuthCodeRequest;
import com.xmonster.howtaxing.dto.sms.SmsMessageBodyRequest;
import com.xmonster.howtaxing.dto.sms.SmsMessageBodyRequest.Message;
import com.xmonster.howtaxing.dto.sms.SmsMessageBodyResponse;
import com.xmonster.howtaxing.dto.sms.SmsSendAuthCodeRequest;
import com.xmonster.howtaxing.feign.sms.SmsSendApi;
import com.xmonster.howtaxing.model.SmsAuthInfo;
import com.xmonster.howtaxing.repository.sms.SmsAuthRepository;
import com.xmonster.howtaxing.type.ErrorCode;
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
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SmsAuthService {
    private final SmsSendApi smsSendApi;
    private final SmsAuthRepository smsAuthRepository;

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

        String id = smsSendAuthCodeRequest.getId();
        String phoneNumber = smsSendAuthCodeRequest.getPhoneNumber().replace(HYPHEN, EMPTY);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todaySendCount = smsAuthRepository.countByPhoneNumberAndSendDatetimeBetween(phoneNumber, startOfDay, endOfDay);

        // 인증번호 10회 이상 발송 불가
        if(todaySendCount > 10) throw new CustomException(ErrorCode.SMS_AUTH_COUNT_ERROR);

        // 인증번호 생성
        String authCode = generateAuthCode();

        // 인증번호 발송(Naver Cloud Platform)
        SmsMessageBodyResponse smsMessageBodyResponse = this.sendSmsByNaverCloudPlatform(phoneNumber, authCode);

        // 인증번호 발송 실패
        if(smsMessageBodyResponse == null || !SMS_STATUS_CODE_SUCCESS.equals(smsMessageBodyResponse.getStatusCode())){
            throw new CustomException(ErrorCode.SMS_AUTH_SEND_ERROR);
        }

        // 인증정보 저장
        smsAuthRepository.saveAndFlush(
                SmsAuthInfo.builder()
                        .socialId(id)
                        .phoneNumber(phoneNumber)
                        .authCode(authCode)
                        .sendDatetime(LocalDateTime.now())
                        .build());

        return ApiResponse.success(Map.of("result", "입력하신 휴대폰 번호로 인증번호를 발송했어요."));
    }

    // 인증번호 검증
    public Object checkAuthCode(SmsCheckAuthCodeRequest smsCheckAuthCodeRequest) throws Exception {
        log.info(">> [Service]SmsAuthService checkAuthCode - 인증번호 검증");

        // 인증번호 검증 유효값 체크
        this.validationCheckForCheckAuthCode(smsCheckAuthCodeRequest);

        String phoneNumber = smsCheckAuthCodeRequest.getPhoneNumber().replace(HYPHEN, EMPTY);
        String authCode = smsCheckAuthCodeRequest.getAuthCode();
        
        SmsAuthInfo smsAuthInfo = smsAuthRepository.findTopByPhoneNumberOrderBySendDatetimeDesc(phoneNumber);
        if(smsAuthInfo == null){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "발송된 인증번호가 없습니다.");
        }

        LocalDateTime sendDatetime = smsAuthInfo.getSendDatetime();
        String orgAuthCode = StringUtils.defaultString(smsAuthInfo.getAuthCode());

        // 인증시간 만료(3분)
        if(LocalDateTime.now().minusMinutes(3).isBefore(sendDatetime)){
            throw new CustomException(ErrorCode.SMS_AUTH_TIME_ERROR);
        }

        // 인증번호 불일치
        if(!orgAuthCode.equals(authCode)){
            throw new CustomException(ErrorCode.SMS_AUTH_MATCH_ERROR);
        }

        smsAuthInfo.setAuthDatetime(LocalDateTime.now());
        smsAuthRepository.save(smsAuthInfo);

        return ApiResponse.success(Map.of("result", "인증번호가 일치합니다."));
    }

    // 인증번호 발송 유효값 체크
    private void validationCheckForSendAuthCode(SmsSendAuthCodeRequest smsSendAuthCodeRequest){
        if(smsSendAuthCodeRequest == null){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR);
        }

        String id = smsSendAuthCodeRequest.getId();
        String phoneNumber = smsSendAuthCodeRequest.getPhoneNumber();

        if(StringUtils.isBlank(id)){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "아이디가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.SMS_AUTH_INPUT_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }
    }

    // 인증번호 검증 유효값 체크
    private void validationCheckForCheckAuthCode(SmsCheckAuthCodeRequest smsCheckAuthCodeRequest){
        if(smsCheckAuthCodeRequest == null){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "인증번호 검증을 위한 입력값이 올바르지 않아요.");
        }

        String phoneNumber = smsCheckAuthCodeRequest.getPhoneNumber();
        String authCode = smsCheckAuthCodeRequest.getAuthCode();

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.SMS_AUTH_CHECK_ERROR, "정확한 휴대폰번호를 입력해주세요.");
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
