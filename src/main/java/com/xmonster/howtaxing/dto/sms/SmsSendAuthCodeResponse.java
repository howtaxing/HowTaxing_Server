package com.xmonster.howtaxing.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsSendAuthCodeResponse {
    private String phoneNumber;         // 전화번호(휴대폰번호)
    private String id;                  // 아이디(사용자입력ID 또는 소셜ID)
    private String authType;            // 인증유형(JOIN:회원가입, FIND_ID:아이디찾기, RESET_PW:비밀번호재설정)
    private String sendDatetime;        // 발송일시
}
