package com.xmonster.howtaxing.dto.hyphen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HyphenAuthResponse {
    private String access_token;
    private String token_type;
    private String expires_in;
    private String scope;
}