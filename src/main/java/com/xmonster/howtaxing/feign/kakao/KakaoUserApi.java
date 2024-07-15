package com.xmonster.howtaxing.feign.kakao;

import com.xmonster.howtaxing.config.FeignConfiguration;
import com.xmonster.howtaxing.dto.user.SocialLogoutAndUnlinkRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "kakaoUser", url="https://kapi.kakao.com", configuration = {FeignConfiguration.class})
public interface KakaoUserApi {
    @GetMapping("/v2/user/me")
    ResponseEntity<String> getUserInfo(@RequestHeader Map<String, String> header);

    @PostMapping("/v1/user/logout")
    ResponseEntity<String> logoutUserInfo(@RequestHeader Map<String, String> header, @RequestBody SocialLogoutAndUnlinkRequest socialLogoutAndUnlinkRequest);

    @PostMapping("/v1/user/unlink")
    ResponseEntity<String> unlinkUserInfo(@RequestHeader Map<String, String> header, @RequestBody SocialLogoutAndUnlinkRequest socialLogoutAndUnlinkRequest);
}

