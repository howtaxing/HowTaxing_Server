package com.xmonster.howtaxing.dto.hyphen;

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
    String type;            // 검색타입
    String roadAddress;     // 도로명주소
    String jibunAddress;    // 지번주소
    String area;            // 면적
}
