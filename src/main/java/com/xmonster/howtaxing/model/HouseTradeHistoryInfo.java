package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 부동산거래내역정보
 *  - 하이픈을 통해 청약홈에서 보유주택정보를 조회한 데이터 중 부동산거래내역정보를 저장
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HouseTradeHistoryInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long tradeHistoryId;                // 거래내역ID

    private Long searchId;                      // 조회ID
    private String name;                        // 성명
    private String address;                     // 주소
    private String sellBuyClassification;       // 매도/매수구분
    private String area;                        // 전용면적(m^2)
    private String tradingPrice;                // 매매가
    private String balancePaymentDate;          // 잔금지급일
    private String contractDate;                // 계약일자
    private String startDate;                   // 신고분기준 시작일
    private String endDate;                     // 신고분기준 종료일
}