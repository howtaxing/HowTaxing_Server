package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * 계산해설응답이력
 *  - 취득세 및 양도소득세 계산 시 해설 응답 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationCommentaryResponseHistory implements Serializable {
    @EmbeddedId
    private CalculationHistoryId calculationHistoryId;

    private String commentaryContent;   // 해설내용
}
