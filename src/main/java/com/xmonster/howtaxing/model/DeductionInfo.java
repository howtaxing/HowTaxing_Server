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
    private String unit;            // 단위
    private Double unitDedRate;     // 단위공제율
    private Integer limitYear;      // 한도연수
    private Double limitDedRate;    // 한도공제율
    private String remark;          // 비고
}
