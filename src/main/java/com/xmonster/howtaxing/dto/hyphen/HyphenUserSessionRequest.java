package com.xmonster.howtaxing.dto.hyphen;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyphenUserSessionRequest {
    private String type;            // 검색타입
    private String roadAddress;     // 도로명주소
    private String jibunAddress;    // 지번주소
    private String area;            // 면적
}
