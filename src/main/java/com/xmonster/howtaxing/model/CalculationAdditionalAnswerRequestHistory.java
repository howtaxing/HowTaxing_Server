package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 계산추가답변요청이력
 *  - 취득세 및 양도소득세 계산 시 추가질의 답변 요청 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationAdditionalAnswerRequestHistory implements Serializable {
    @EmbeddedId
    private CalculationHistoryId calculationHistoryId;

    private String questionId;      // 질의ID
    private String answerValue;     // 응답값
}
