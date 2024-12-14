package com.xmonster.howtaxing.dto.sms;

import com.xmonster.howtaxing.type.AuthType;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SmsSendAuthCodeRequest {
    private String phoneNumber;     // [필수] 전화번호(휴대폰번호)
    private String id;              // [선택] 아이디(사용자입력ID 또는 소셜ID)
    private String authType;        // [필수] 인증유형(JOIN:회원가입, FIND_ID:아이디찾기, RESET_PW:비밀번호재설정)
}
