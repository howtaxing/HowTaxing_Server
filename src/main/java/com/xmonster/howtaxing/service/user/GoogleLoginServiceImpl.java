package com.xmonster.howtaxing.service.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xmonster.howtaxing.dto.user.GoogleLoginResponse;
import com.xmonster.howtaxing.dto.user.GoogleRequestAccessTokenDto;
import com.xmonster.howtaxing.dto.user.SocialAuthResponse;
import com.xmonster.howtaxing.dto.user.SocialUserResponse;
import com.xmonster.howtaxing.feign.google.GoogleAuthApi;
import com.xmonster.howtaxing.feign.google.GoogleUserApi;
import com.xmonster.howtaxing.type.SocialType;
import com.xmonster.howtaxing.utils.GsonLocalDateTimeAdapter;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
@Qualifier("googleLogin")
public class GoogleLoginServiceImpl implements SocialLoginService {
  private final GoogleAuthApi googleAuthApi;
  private final GoogleUserApi googleUserApi;

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String googleAppKey;
  @Value("${spring.security.oauth2.client.registration.google.client-secret}")
  private String googleAppSecret;
  @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
  private String googleRedirectUri;
  @Value("${spring.security.oauth2.client.registration.google.authorization-grant-type}")
  private String googleGrantType;

  @Override
  public SocialType getServiceName() {
    return SocialType.GOOGLE;
  }

  @Override
  public SocialAuthResponse getAccessToken(String authorizationCode) {
    ResponseEntity<?> response = googleAuthApi.getAccessToken(
        GoogleRequestAccessTokenDto.builder()
            .code(authorizationCode)
            .client_id(googleAppKey)
            .clientSecret(googleAppSecret)
            .redirect_uri(googleRedirectUri)
            .grant_type(googleGrantType)
            .build()
    );

    log.info("google auth info");
    log.info(response.toString());

    return new Gson()
        .fromJson(
            response.getBody().toString(),
            SocialAuthResponse.class
        );
  }

  @Override
  public SocialUserResponse getUserInfo(String accessToken) {
    ResponseEntity<?> response = googleUserApi.getUserInfo(accessToken);

    log.info("google user response");
    log.info(response.toString());

    String jsonString = response.getBody().toString();

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter())
        .create();

    GoogleLoginResponse googleLoginResponse = gson.fromJson(jsonString, GoogleLoginResponse.class);

    return SocialUserResponse.builder()
        .id(googleLoginResponse.getId())
        .email(googleLoginResponse.getEmail())
        .build();
  }
}
