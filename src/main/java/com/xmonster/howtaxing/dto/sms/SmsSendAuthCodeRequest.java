package com.xmonster.howtaxing.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsSendAuthCodeRequest {
    private String phoneNumber;     // [필수] 전화번호(휴대폰번호)
    private String id;              // [선택] 아이디(사용자입력ID 또는 소셜ID)
}
