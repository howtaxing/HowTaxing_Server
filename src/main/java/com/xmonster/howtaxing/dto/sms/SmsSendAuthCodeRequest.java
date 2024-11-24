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
    private String id;              // 아이디(사용자입력ID 또는 소셜ID)
    private String phoneNumber;     // 전화번호(휴대폰번호)
}
