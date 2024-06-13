package com.xmonster.howtaxing.service.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xmonster.howtaxing.dto.user.NaverLoginResponse;
import com.xmonster.howtaxing.dto.user.SocialAuthResponse;
import com.xmonster.howtaxing.dto.user.SocialUserResponse;
import com.xmonster.howtaxing.feign.naver.NaverAuthApi;
import com.xmonster.howtaxing.feign.naver.NaverUserApi;
import com.xmonster.howtaxing.type.SocialType;
import com.xmonster.howtaxing.utils.GsonLocalDateTimeAdapter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
@Qualifier("naverLogin")
public class NaverLoginServiceImpl implements SocialLoginService {

  private final NaverAuthApi naverAuthApi;
  private final NaverUserApi naverUserApi;

  @Value("${spring.security.oauth2.client.registration.naver.client-id}")
  private String naverAppKey;
  @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
  private String naverAppSecret;
  @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
  private String naverRedirectUri;
  @Value("${spring.security.oauth2.client.registration.naver.authorization-grant-type}")
  private String naverGrantType;


  @Override
  public SocialType getServiceName() {
    return SocialType.NAVER;
  }

  @Override
  public SocialAuthResponse getAccessToken(String authorizationCode) {
    ResponseEntity<?> response = naverAuthApi.getAccessToken(
        naverGrantType,
        naverAppKey,
        naverAppSecret,
        authorizationCode,
        "state"
    );

    log.info("naver auth response {}", response.toString());

    return new Gson()
        .fromJson(
            String.valueOf(response.getBody())
            , SocialAuthResponse.class
        );
  }

  @Override
  public SocialUserResponse getUserInfo(String accessToken) {
    Map<String ,String> headerMap = new HashMap<>();
    headerMap.put("authorization", "Bearer " + accessToken);

    ResponseEntity<?> response = naverUserApi.getUserInfo(headerMap);

    log.info("naver user response");
    log.info(response.toString());

    String jsonString = response.getBody().toString();

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter())
        .create();

    NaverLoginResponse naverLoginResponse = gson.fromJson(jsonString, NaverLoginResponse.class);
    NaverLoginResponse.Response naverUserInfo = naverLoginResponse.getResponse();

    return SocialUserResponse.builder()
        .id(naverUserInfo.getId())
        .gender(naverUserInfo.getGender())
        .name(naverUserInfo.getName())
        .email(naverUserInfo.getEmail())
        .build();
  }
}
