package com.xmonster.howtaxing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AdjustmentTargetAreaInfo extends DateEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long adjId;             // 조정대상지역ID

    private LocalDate startDate;    // 개시일자
    private LocalDate endDate;      // 종료일자
    private String targetArea;      // 대상주소
    private Boolean hasDetail;      // 상세존재여부
}
