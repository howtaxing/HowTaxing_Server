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
    private String authType;        // 인증유형(JOIN:회원가입, FIND_ID:아이디찾기, RESET_PW:비밀번호재설정)
    private String authCode;        // 인증번호
}
