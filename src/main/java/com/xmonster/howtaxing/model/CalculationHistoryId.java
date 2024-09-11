package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Getter
@Builder
public class CalculationHistoryId implements Serializable {
    private Long calcHistoryId;         // 상담자ID
    private Integer detailHistorySeq;   // 상세이력번호
}
