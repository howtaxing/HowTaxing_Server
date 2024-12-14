package com.xmonster.howtaxing.dto.user;

import com.xmonster.howtaxing.type.Role;
import com.xmonster.howtaxing.type.SocialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocialLoginResponse {
    private String accessToken;
    private String refreshToken;
    private Role role;
}
