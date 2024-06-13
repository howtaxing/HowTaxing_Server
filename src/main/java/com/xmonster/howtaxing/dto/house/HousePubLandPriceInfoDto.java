package com.xmonster.howtaxing.dto.house;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HousePubLandPriceInfoDto {
    private String baseYear;            // 기준연도
    private String baseMonth;           // 기준월
    private String legalDstCode;        // 법정동코드
    private String roadAddr;            // 도로명주소
    private String siDo;                // 시도
    private String siGunGu;             // 시군구
    private String eupMyun;             // 읍면
    private String dongRi;              // 동리
    private String specialLandCode;     // 특수지코드
    private String bonNo;               // 본번
    private String bueNo;               // 부번
    private String specialLandName;     // 특수지명
    private String complexName;         // 단지명
    private String dongName;            // 동명
    private String hoName;              // 호명
    private BigDecimal area;            // 전용면적
    private Long pubLandPrice;          // 공시가격
    private String complexCode;         // 단지코드
    private String dongCode;            // 동코드
    private String hoCode;              // 호코드
}
