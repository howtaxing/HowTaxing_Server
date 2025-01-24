package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class CalculationSellHouseResponse {
    private String houseType;                   // 주택유형
    private String houseName;                   // 주택명
    private String detailAdr;                   // 상세주소

    private LocalDate contractDate;             // 취득계약일자
    private LocalDate buyDate;                  // 취득일자
    private Long buyPrice;                      // 취득금액

    private Long pubLandPrice;                  // 공시가격
    private Boolean isPubLandPriceOver100Mil;   // 공시가격1억초과여부

    private String roadAddr;                    // 도로명주소

    private Double area;                        // 전용면적
    private Boolean isAreaOver85;               // 전용면적85제곱미터초과여부
    private Boolean isDestruction;              // 멸실여부
    private Integer ownerCnt;                   // 소유자수
    private Integer userProportion;             // 본인지분비율
    private Boolean isMoveInRight;              // 입주권여부
}
