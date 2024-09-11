package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Getter
@Setter
@Builder
public class CalculationOwnHouseHistoryId implements Serializable {
    private Long ownHouseHistoryId;     // 보유주택이력ID
    private Integer detailHistorySeq;   // 상세이력번호
}
