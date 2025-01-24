package com.xmonster.howtaxing.feign.apple;

import com.xmonster.howtaxing.config.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(value = "appleUser", url="https://appleid.apple.com", configuration = {FeignConfiguration.class})
public interface AppleAuthApi {
    @GetMapping("/auth/keys")
    Map<String, Object> getApplePublicKey();
    //ResponseEntity<String> getApplePublicKey();
}
