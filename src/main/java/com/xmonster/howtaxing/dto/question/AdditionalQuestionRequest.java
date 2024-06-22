package com.xmonster.howtaxing.dto.question;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdditionalQuestionRequest {
    private String calcType;        // [필수] 계산유형(01:취득세, 02:양도소득세)
    private String questionId;      // [선택] 질의ID
    private String answerValue;     // [선택] 응답값
    private Long sellHouseId;       // [선택] 양도주택ID
    private LocalDate sellDate;     // [선택] 양도일자
    private Long sellPrice;         // [선택] 양도가액
    private Long ownHouseCnt;       // [선택] 보유주택수
}