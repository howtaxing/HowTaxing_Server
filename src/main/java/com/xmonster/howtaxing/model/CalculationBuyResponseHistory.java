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

    private String userProportion;      // 본인지분비율
    private String buyPrice;            // 취득가액
    private String buyTaxRate;          // 취득세율
    private String buyTaxPrice;         // 취득세액
    private String eduTaxRate;          // 지방교육세율
    private String eduTaxPrice;         // 지방교육세액
    private String eduDiscountPrice;    // 지방교육세감면액
    private String agrTaxRate;          // 농어촌특별세율
    private String agrTaxPrice;         // 농어촌특별세액
    private String totalTaxPrice;       // 총납부세액
}
