package com.xmonster.howtaxing.controller.sms;

import com.xmonster.howtaxing.dto.sms.SmsCheckAuthCodeRequest;
import com.xmonster.howtaxing.dto.sms.SmsSendAuthCodeRequest;
import com.xmonster.howtaxing.service.sms.SmsAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SmsAuthController {
    private final SmsAuthService smsAuthService;
    
    // 인증번호 발송
    @PostMapping("/sms/sendAuthCode")
    public Object sendAuthCode(@RequestBody SmsSendAuthCodeRequest smsSendAuthCodeRequest) throws Exception {
        log.info(">> [Controller]SmsAuthController sendAuthCode - 인증번호 발송");
        return smsAuthService.sendAuthCode(smsSendAuthCodeRequest);
    }
    
    // 인증번호 검증
    @PostMapping("/sms/checkAuthCode")
    public Object checkAuthCode(@RequestBody SmsCheckAuthCodeRequest smsCheckAuthCodeRequest) throws Exception {
        log.info(">> [Controller]SmsAuthController checkAuthCode - 인증번호 검증");
        return smsAuthService.checkAuthCode(smsCheckAuthCodeRequest);
    }
}
