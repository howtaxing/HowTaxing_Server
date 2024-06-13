package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
// CSV 등록용(20240407 현재 기준 미사용)
public class HousePubLandPriceInfo extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long housePubLandPriceId;   // 주택ID

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