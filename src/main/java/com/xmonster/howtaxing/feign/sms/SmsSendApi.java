package com.xmonster.howtaxing.feign.sms;

import com.xmonster.howtaxing.config.FeignConfiguration;
import com.xmonster.howtaxing.dto.sms.SmsMessageBodyRequest;
import com.xmonster.howtaxing.dto.user.SocialLogoutAndUnlinkRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "smsSend", url="https://sens.apigw.ntruss.com/sms/v2/services/ncp:sms:kr:320009612043:howtaxing", configuration = {FeignConfiguration.class})
public interface SmsSendApi {
    @PostMapping("/messages")
    ResponseEntity<String> sendSms(@RequestHeader Map<String, Object> header, @RequestBody SmsMessageBodyRequest smsMessageBodyRequest);
}

