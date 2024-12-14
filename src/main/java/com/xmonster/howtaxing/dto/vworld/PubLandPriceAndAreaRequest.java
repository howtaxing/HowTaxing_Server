package com.xmonster.howtaxing.dto.vworld;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PubLandPriceAndAreaRequest {
    private String legalDstCode;    // [필수] 법정동코드
    private String roadAddr;        // [필수] 도로명주소
    private String siDo;            // 시도
    private String siGunGu;         // 시군구
    private String complexName;     // 단지명
    private String dongName;        // [선택] 동명
    private String hoName;          // [필수] 호명
}