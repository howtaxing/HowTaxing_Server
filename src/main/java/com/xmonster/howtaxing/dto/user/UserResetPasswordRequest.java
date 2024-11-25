package com.xmonster.howtaxing.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserResetPasswordRequest {
    private String id;              // [필수] 아이디
    private String phoneNumber;     // [필수] 휴대폰번호(ex:01012345678)
    private String authKey;         // [필수] 인증키(SMS인증 후 받은 인증키 30자리)
}
