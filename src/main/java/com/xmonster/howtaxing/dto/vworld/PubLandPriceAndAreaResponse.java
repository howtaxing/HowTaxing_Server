package com.xmonster.howtaxing.dto.vworld;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PubLandPriceAndAreaResponse {
    private String complexName;     // 단지명
    private String dongName;        // 동
    private String hoName;          // 호
    private BigDecimal area;        // 전용면적
    private Long pubLandPrice;      // 공시가격
}