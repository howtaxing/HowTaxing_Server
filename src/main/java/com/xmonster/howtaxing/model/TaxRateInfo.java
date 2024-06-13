package com.xmonster.howtaxing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TaxRateInfo extends DateEntity implements Serializable {
    @Id
    @Column
    private String taxRateCode;     // 세율코드

    private String taxRateName;     // 세율명
    private String constYn;         // 상수여부
    private String usedFunc;        // 사용함수
    private Long basePrice;         // 기준금액
    private String taxRate1;        // 세율1
    private String addTaxRate1;     // 추가세율1
    private String taxRate2;        // 세율2
    private String addTaxRate2;     // 추가세율2
    private String remark;          // 비고
}
