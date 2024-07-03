package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class CalculationAdditionalAnswerRequest {
    private String questionId;      // 질의ID
    private String answerValue;     // 응답값
}
