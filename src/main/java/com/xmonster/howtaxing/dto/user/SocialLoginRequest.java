package com.xmonster.howtaxing.dto.user;

import com.xmonster.howtaxing.type.SocialType;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
public class SocialLoginRequest {
    @NotNull
    private SocialType socialType;
    @NotNull
    private String accessToken;
    //private String code;
}
