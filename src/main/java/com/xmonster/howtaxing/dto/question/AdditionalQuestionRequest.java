package com.xmonster.howtaxing.dto.question;

import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultRequest;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultRequest;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class AdditionalQuestionRequest {
    private String calcType;        // [필수] 계산유형(01:취득세, 02:양도소득세)
    private String questionId;      // [선택] 질의ID
    private String answerValue;     // [선택] 응답값

    private Long sellHouseId;       // [선택] 양도주택ID  (양도소득세 계산)
    private LocalDate sellDate;     // [선택] 양도일자    (양도소득세 계산)
    private Long sellPrice;         // [선택] 양도가액    (양도소득세 계산)

    private Long ownHouseCnt;       // [선택] 보유주택수  (취득세 계산)
    private LocalDate buyDate;      // [선택] 취득일자    (취득세 계산)
    private String jibunAddr;       // [선택] 지번주소    (취득세 계산)

    // TODO. 이후에는 각 계산에 대한 request를 받는게 좋을듯
    //CalculationBuyResultRequest calculationBuyResultRequest;
    //CalculationSellResultRequest calculationSellResultRequest;
}