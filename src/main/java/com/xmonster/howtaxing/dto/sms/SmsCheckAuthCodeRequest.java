package com.xmonster.howtaxing.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsCheckAuthCodeRequest {
    private String phoneNumber;     // 전화번호(휴대폰번호)
    private String authCode;        // 인증번호
}
