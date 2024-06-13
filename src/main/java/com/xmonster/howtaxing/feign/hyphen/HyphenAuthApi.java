package com.xmonster.howtaxing.feign.hyphen;

import com.xmonster.howtaxing.config.FeignConfiguration;
import com.xmonster.howtaxing.dto.hyphen.HyphenRequestAccessTokenDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "hyphenAuth", url="https://api.hyphen.im", configuration = {FeignConfiguration.class})
public interface HyphenAuthApi {
    @PostMapping("/oauth/token")
    ResponseEntity<String> getAccessToken(@RequestBody HyphenRequestAccessTokenDto requestDto);
}
