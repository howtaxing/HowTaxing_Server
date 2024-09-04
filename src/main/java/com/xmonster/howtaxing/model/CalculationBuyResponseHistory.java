package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 계산취득세응답이력
 *  - 취득세 계산 응답 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationBuyResponseHistory implements Serializable {
    @EmbeddedId
    private CalculationHistoryId calculationHistoryId;

    private Long buyPrice;              // 취득가액
    private Double buyTaxRate;          // 취득세율
    private Long buyTaxPrice;           // 취득세액
    private Double eduTaxRate;          // 지방교육세율
    private Long eduTaxPrice;           // 지방교육세액
    private Long eduDiscountPrice;      // 지방교육세감면액
    private Double agrTaxRate;          // 농어촌특별세율
    private Long agrTaxPrice;           // 농어촌특별세액
    private Long totalTaxPrice;         // 총납부세액
}
