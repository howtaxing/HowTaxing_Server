package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationSellResult extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long calculationResultId;   // 계산결과ID

    private Long userId;                // 사용자ID
    private Long buyPrice;              // 취득가액
    private LocalDate buyDate;          // 취득일자
    private Long sellPrice;             // 양도가액
    private LocalDate sellDate;         // 양도일자
    private Long necExpensePrice;       // 필요경비금액
    private Long sellProfitPrice;       // 양도차익금액
    private Integer retentionPeriod;    // 보유기간
    private Long taxableStdPrice;       // 과세표준금액
    private Double sellTaxRate;         // 양도소득세율
    private Long sellTaxPrice;          // 양도소득세액
    private Long localTaxPrice;         // 지방소득세액
    private Long nonTaxablePrice;       // 비과세대상양도차익금액
    private Long taxablePrice;          // 과세대상양도차익금액
    private Long longDeductionPrice;    // 장기보유특별공제금액
    private Long sellIncomePrice;       // 양도소득금액
    private Long basicDeductionPrice;   // 기본공제금액
    private Long progDeductionPrice;    // 누진공제금액
    private Long totalTaxPrice;         // 총납부세액
}
