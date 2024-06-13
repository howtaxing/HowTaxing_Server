package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalculationSellResultResponse {
    private int listCnt;
    private List<CalculationSellOneResult> list;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CalculationSellOneResult {
        private String buyPrice;            // 취득가액
        private String buyDate;             // 취득일자
        private String sellPrice;           // 양도가액
        private String sellDate;            // 양도일자
        private String necExpensePrice;     // 필요경비금액
        private String sellProfitPrice;     // 양도차익금액
        private String retentionPeriod;     // 보유기간

        private String nonTaxablePrice;     // 비과세대상양도차익금액
        private String taxablePrice;        // 과세대상양도차익금액
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
}
