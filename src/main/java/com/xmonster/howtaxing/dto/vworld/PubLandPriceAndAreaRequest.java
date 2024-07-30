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
    private String siDo;
    private String siGunGu;
    private String complexName;
    private String dongName;        // [선택] 동명
    private String hoName;          // [필수] 호명
}