package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class CalculationSellResultRequest {
    private Long houseId;                           // [필수] (양도)주택ID
    private LocalDate sellContractDate;             // [필수] (양도)계약일자
    private LocalDate sellDate;                     // [필수] 양도일자
    private Long sellPrice;                         // [필수] 양도금액
    private Long necExpensePrice;                   // [필수] 필요경비금액
    
    // 추가질의답변리스트
    private List<CalculationAdditionalAnswerRequest> additionalAnswerList;
}
