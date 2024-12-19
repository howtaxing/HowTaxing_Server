package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 계산취득세요청이력
 *  - 취득세 계산 요청 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationBuyRequestHistory implements Serializable {
    @Id
    @Column
    private Long calcHistoryId;                 // 상담자ID

    private String houseType;                   // 주택유형
    private String houseName;                   // 주택명
    private String detailAdr;                   // 상세주소
    private LocalDate contractDate;             // 계약일자
    private LocalDate balanceDate;              // 잔금지급일자
    private LocalDate buyDate;                  // 취득일자
    private Long buyPrice;                      // 취득금액
    private Long pubLandPrice;                  // 공시가격
    private Boolean isPubLandPriceOver100Mil;   // 공시가격1억초과여부
    private Double area;                        // 전용면적
    private Boolean isAreaOver85;               // 전용면적85제곱미터초과여부

    private String jibunAddr;                   // 지번주소
    private String roadAddr;                    // 도로명주소
    private String bdMgtSn;                     // 건물관리번호
    private String admCd;                       // 행정구역코드
    private String rnMgtSn;                     // 도로명코드

    private Boolean isDestruction;              // 멸실여부
    private Integer ownerCnt;                   // 소유자수
    private Integer userProportion;             // 본인지분비율
    private Boolean isMoveInRight;              // 입주권여부
}
