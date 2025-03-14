package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 계산보유주택이력상세
 *  - 취득세 및 양도소득세 계산 시 보유주택 이력 상세정보
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationOwnHouseHistoryDetail implements Serializable {
    @EmbeddedId
    private CalculationOwnHouseHistoryId calculationOwnHouseHistoryId;

    private Long houseId;               // 주택ID
    private String houseType;           // 주택유형(1:아파트 2:연립,다가구 3:입주권 4:단독주택,다세대 5:분양권(주택) 6:주택)
    private String houseName;           // 주택명
    private String detailAdr;           // 상세주소
    private LocalDate contractDate;     // 계약일자
    private LocalDate balanceDate;      // 잔금지급일자
    private LocalDate buyDate;          // 취득일자
    private Long buyPrice;              // 취득금액
    private Long pubLandPrice;          // 공시가격
    private BigDecimal area;            // 전용면적
    private Long kbMktPrice;            // KB시세
    private String jibunAddr;           // 지번주소
    private String roadAddr;            // 도로명주소
    private String roadAddrRef;         // 도로명주소참고항목
    private String bdMgtSn;             // 건물관리번호
    private String admCd;               // 행정구역코드
    private String rnMgtSn;             // 도로명코드
    private Boolean isDestruction;      // 멸실여부
    private Integer ownerCnt;           // 소유자수
    private Integer userProportion;     // 본인지분비율
    private Boolean isMoveInRight;      // 입주권여부
    private String sourceType;          // 출처유형
}
