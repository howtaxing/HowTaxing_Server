package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 계산양도소득세요청이력
 *  - 양도소득세 계산 요청 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationSellRequestHistory implements Serializable {
    @EmbeddedId
    private CalculationHistoryId calculationHistoryId;

    private Long sellHouseId;                   // 양도주택ID
    private LocalDate sellContractDate;         // 양도계약일자
    private LocalDate sellDate;                 // 양도일자
    private Long sellPrice;                     // 양도금액
    private Long necExpensePrice;               // 필요경비금액
}
