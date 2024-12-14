package com.xmonster.howtaxing.service.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.sms.*;
import com.xmonster.howtaxing.dto.sms.SmsMessageBodyRequest.Message;
import com.xmonster.howtaxing.feign.sms.SmsSendApi;
import com.xmonster.howtaxing.model.SmsAuthInfo;
import com.xmonster.howtaxing.repository.sms.SmsAuthRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xmonster.howtaxing.constant.CommonConstant.EMPTY;
import static com.xmonster.howtaxing.constant.CommonConstant.HYPHEN;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SmsMessageService {
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

    // 메시지 발송
    public boolean sendMessage(SmsSendMessageRequest smsSendMessageRequest) throws Exception {
        log.info(">> [Service]SmsMessageService sendMessage - 메시지 발송");

        // 메시지 발송 유효값 체크
        this.validationCheckForSendMessage(smsSendMessageRequest);

        String phoneNumber = smsSendMessageRequest.getPhoneNumber().replace(HYPHEN, EMPTY);
        String messageContent = smsSendMessageRequest.getMessageContent();


        // 메시지 발송(Naver Cloud Platform)
        SmsMessageBodyResponse smsMessageBodyResponse = this.sendSmsByNaverCloudPlatform(phoneNumber, messageContent);

        // 메시지 발송 실패
        if(smsMessageBodyResponse == null || !SMS_STATUS_CODE_SUCCESS.equals(smsMessageBodyResponse.getStatusCode())){
            throw new CustomException(ErrorCode.SMS_AUTH_SEND_ERROR);
        }

        return true;
    }

    // 인증번호 발송 유효값 체크
    private void validationCheckForSendMessage(SmsSendMessageRequest smsSendMessageRequest){
        if(smsSendMessageRequest == null){
            throw new CustomException(ErrorCode.SMS_MSG_INPUT_ERROR);
        }

        String phoneNumber = smsSendMessageRequest.getPhoneNumber();
        String messageContent = smsSendMessageRequest.getMessageContent();

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.SMS_MSG_INPUT_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.SMS_MSG_INPUT_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }
        
        if(StringUtils.isBlank(messageContent)){
            throw new CustomException(ErrorCode.SMS_MSG_INPUT_ERROR, "메시지 내용이 입력되지 않았습니다.");
        }
    }

    // 네이버 클라우드 플랫폼을 통한 SMS 발송
    private SmsMessageBodyResponse sendSmsByNaverCloudPlatform(String phoneNumber, String messageContent) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        Long time = System.currentTimeMillis();
        Map<String, Object> headerMap = new HashMap<>();

        headerMap.put("x-ncp-apigw-timestamp", time.toString());
        headerMap.put("x-ncp-iam-access-key", this.accessKey);
        headerMap.put("x-ncp-apigw-signature-v2", this.makeSignature(time));

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
