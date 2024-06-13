package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Getter
@Builder
public class CalculationProcessId implements Serializable {
    private String calcType;    // 계산구분
    private String branchNo;    // 분기번호
    private Integer selectNo;   // 선택번호
}
