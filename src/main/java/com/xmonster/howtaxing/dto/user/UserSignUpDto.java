package com.xmonster.howtaxing.dto.user;

import lombok.*;

// (GGMANYAR) - TOBE
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSignUpDto {
    private String joinType;        // [필수] 가입유형("IDPASS":아이디/비밀번호 회원가입, "SOCIAL":소셜 회원가입)
    private String id;              // [선택] 아이디(joinType이 'IDPASS' 인 경우 필수)
    private String password;        // [선택] 비밀번호(joinType이 'IDPASS' 인 경우 필수)
    private String email;           // [선택] 이메일(현재 미사용)
    private String phoneNumber;     // [필수] 휴대폰번호(ex:01012345678)
    private Boolean mktAgr;         // [필수] 마케팅동의여부(1:여, 0:부)
    private String authKey;         // [필수] 인증키(SMS인증 후 받은 인증키 30자리)
}
