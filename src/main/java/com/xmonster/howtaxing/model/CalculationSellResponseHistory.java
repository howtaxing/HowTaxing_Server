package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * 계산양도소득세응답이력
 *  - 양도소득세 계산 응답 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationSellResponseHistory implements Serializable {
    @EmbeddedId
    private CalculationHistoryId calculationHistoryId;

    private String buyPrice;            // 취득가액
    private String buyDate;             // 취득일자
    private String sellPrice;           // 양도가액
    private String sellDate;            // 양도일자
    private String necExpensePrice;     // 필요경비금액
    private String sellProfitPrice;     // 양도차익금액
    private String retentionPeriod;     // 보유기간
    private String taxablePrice;        // 과세대상양도차익금액
    private String nonTaxablePrice;     // 비과세대상양도차익금액
    private String longDeductionPrice;  // 장기보유특별공제금액
    private String sellIncomePrice;     // 양도소득금액
    private String basicDeductionPrice; // 기본공제금액
    private String taxableStdPrice;     // 과세표준금액
    private String sellTaxRate;         // 양도소득세율
    private String progDeductionPrice;  // 누진공제금액
    private String sellTaxPrice;        // 양도소득세액
    private String localTaxPrice;       // 지방소득세액
    private String totalTaxPrice;       // 총납부세액
}
