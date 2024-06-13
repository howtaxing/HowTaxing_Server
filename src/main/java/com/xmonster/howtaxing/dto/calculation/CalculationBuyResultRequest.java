package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalculationBuyResultRequest {
    private String houseType;                   // [필수] 주택유형
    private String houseName;                   // [필수] 주택명
    private String detailAdr;                   // [필수] 상세주소

    private LocalDate contractDate;             // [선택] 계약일자
    private LocalDate balanceDate;              // [선택] 잔금지급일자
    private LocalDate buyDate;                  // [필수] 취득일자
    private Long buyPrice;                      // [필수] 취득금액

    private Long pubLandPrice;                  // [선택] 공시지가
    private Boolean isPubLandPriceOver100Mil;   // [필수] 공시지가1억초과여부

    private String jibunAddr;                   // [선택] 지번주소
    private String roadAddr;                    // [필수] 도로명주소
    private String roadAddrRef;                 // [선택] 도로명주소참고항목
    private String bdMgtSn;                     // [선택] 건물관리번호
    private String admCd;                       // [선택] 행정구역코드
    private String rnMgtSn;                     // [선택] 도로명코드

    private Double area;                        // [선택] 전용면적
    private Boolean isAreaOver85;               // [필수] 전용면적85제곱미터초과여부
    private Boolean isDestruction;              // [필수] 멸실여부
    private Integer ownerCnt;                   // [필수] 소유자수
    private Integer userProportion;             // [필수] 본인지분비율
    private Boolean isMoveInRight;              // [필수] 입주권여부
    private Boolean hasSellPlan;                // [필수] 양도예정여부

    private Boolean isOwnHouseCntRegist;        // [필수] 보유주택수직접입력여부
    private Integer ownHouseCnt;                // [선택] 보유주택수
}
