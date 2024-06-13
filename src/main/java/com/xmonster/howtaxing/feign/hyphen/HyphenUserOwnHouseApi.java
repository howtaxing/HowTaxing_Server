package com.xmonster.howtaxing.feign.hyphen;

import com.xmonster.howtaxing.config.FeignConfiguration;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserHouseListRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "hyphenUserOwnHouse", url="https://api.hyphen.im", configuration = {FeignConfiguration.class})
public interface HyphenUserOwnHouseApi {
    @PostMapping("/in0148001055")
    ResponseEntity<String> getUserOwnHouseInfo(@RequestHeader Map<String, Object> requestHeader, @RequestBody HyphenUserHouseListRequest requestBody);
}
