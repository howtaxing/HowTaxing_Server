package com.xmonster.howtaxing.service.user;

import com.xmonster.howtaxing.dto.user.SocialAuthResponse;
import com.xmonster.howtaxing.dto.user.SocialUserResponse;
import com.xmonster.howtaxing.type.SocialType;
import org.springframework.stereotype.Service;

@Service
public interface SocialLoginService {
    SocialType getServiceName();
    SocialAuthResponse getAccessToken(String authorizationCode);
    SocialUserResponse getUserInfo(String accessToken);
}
