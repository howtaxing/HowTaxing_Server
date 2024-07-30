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
public class DeductionInfo extends DateEntity implements Serializable {
    @Id
    @Column
    private String dedCode;         // 공제코드
    private String dedContent;      // 공제내용
    private String dedMethod;       // 공제함수

    private String dedTarget1;      // 공제대상1
    private String unit1;           // 단위1
    private Double unitDedRate1;    // 단위공제율1
    private Integer limitYear1;     // 한도연수1
    private Double limitDedRate1;   // 한도공제율1

    private String dedTarget2;      // 공제대상2
    private String unit2;           // 단위2
    private Double unitDedRate2;    // 단위공제율2
    private Integer limitYear2;     // 한도연수2
    private Double limitDedRate2;   // 한도공제율2

    private String remark;          // 비고
}
