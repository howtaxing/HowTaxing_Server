package com.xmonster.howtaxing.feign.hyphen;

import com.xmonster.howtaxing.config.FeignConfiguration;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserResidentRegistrationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "hyphenUserResidentRegistration", url="https://api.hyphen.im", configuration = {FeignConfiguration.class})
public interface HyphenUserResidentRegistrationApi {
    @PostMapping("/in0005000216")
    ResponseEntity<String> getUserResidentRegistrationInfo(@RequestHeader Map<String, Object> requestHeader, @RequestBody HyphenUserResidentRegistrationRequest requestBody);
}
