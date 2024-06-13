package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalculationSellResultRequest {
    private Long houseId;                           // [필수] (양도)주택ID
    private LocalDate sellContractDate;             // [필수] (양도)계약일자
    private LocalDate sellDate;                     // [필수] 양도일자
    private Long sellPrice;                         // [필수] 양도금액
    private Long necExpensePrice;                   // [필수] 필요경비금액
    private Boolean isWWLandLord;                   // [선택] 상생임대인여부
    private Long stayPeriodYear;                    // [선택] (양도주택)거주기간(년)
    private Long stayPeriodMonth;                   // [선택] (양도주택)거주기간(월)
    private Map<String, Object> planAnswer;         // [선택] 계획여부답변(종전주택 2년이내 매도계획 여부 등)
}
