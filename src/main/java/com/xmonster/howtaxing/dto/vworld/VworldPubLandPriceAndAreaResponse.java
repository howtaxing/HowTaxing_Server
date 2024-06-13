package com.xmonster.howtaxing.dto.vworld;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VworldPubLandPriceAndAreaResponse {
    private Boolean hasPubLandPrice;    // 공시가격 존재여부
    private Long pubLandPrice;          // 공시가격
    private Boolean hasArea;            // 전용면적 존재여부
    private Double area;                // 전용면적
    private String stdrYear;            // 기준연도
}
