package com.xmonster.howtaxing.dto.user;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UserResetPasswordRequest {
    private String phoneNumber;         // [필수] 휴대폰번호(ex:01012345678)
    private String authKey;             // [필수] 인증키(SMS인증 후 받은 인증키 30자리)
    private String id;                  // [필수] 아이디
    private String newPassword;         // [필수] 새 비밀번호
    private String newPasswordConfirm;  // [필수] 새 비밀번호 확인
}
