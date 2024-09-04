package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 조정대상지역정보
 *  - 조정대상지역 정보('조정대상지역 상세정보'까지는 현재 적용하지 않음)
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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
