package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주택조합결과정보
 *  - 하이픈을 통해 청약홈에서 보유주택정보를 조회한 데이터를 조합하여 주택조합결과정보를 저장
 *  - 조합 데이터 : 건축물대장장보, 부동산거래내역정보, 재산세정보
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HouseCombinationResultInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long tradeHistoryId;        // 거래내역ID

    private Long searchId;              // 조회ID
    private String houseType;           // 주택유형(1:아파트 2:연립,다가구 3:입주권 4:단독주택,다세대 5:분양권(주택) 6:주택)
    private String houseName;           // 주택명
    private String detailAdr;           // 상세주소

    private LocalDate contractDate;     // 계약일자
    private LocalDate balanceDate;      // 잔금지급일자
    private LocalDate buyDate;          // 취득일자
    private Long buyPrice;              // 취득금액

    private Long pubLandPrice;          // 공시가격
    private BigDecimal area;            // 전용면적

    private String jibunAddr;           // 지번주소
    private String roadAddr;            // 도로명주소
    private String roadAddrRef;         // 도로명주소참고항목
    private String bdMgtSn;             // 건물관리번호
    private String admCd;               // 행정구역코드
    private String rnMgtSn;             // 도로명코드
}