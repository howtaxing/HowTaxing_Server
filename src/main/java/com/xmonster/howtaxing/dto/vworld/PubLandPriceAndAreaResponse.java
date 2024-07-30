package com.xmonster.howtaxing.dto.vworld;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PubLandPriceAndAreaResponse {
    private String complexName;
    private String dongName;
    private String hoName;
    private BigDecimal area;
    private Long pubLandPrice;
}