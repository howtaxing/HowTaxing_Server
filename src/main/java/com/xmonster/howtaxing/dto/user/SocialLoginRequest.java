package com.xmonster.howtaxing.dto.user;

import com.xmonster.howtaxing.type.SocialType;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SocialLoginRequest {
    private SocialType socialType;      // [필수] 소셜로그인 유형
    private String accessToken;         // [선택] AccessToken(카카오, 네이버 로그인 시 사용)
    private String identityToken;       // [선택] IdentityToken(애플 로그인 시 사용)
}
