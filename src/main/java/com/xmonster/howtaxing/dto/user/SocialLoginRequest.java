package com.xmonster.howtaxing.dto.user;

import com.xmonster.howtaxing.type.SocialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocialLoginRequest {
    private SocialType socialType;
    private String accessToken;
}
