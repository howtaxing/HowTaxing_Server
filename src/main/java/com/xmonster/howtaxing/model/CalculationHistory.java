package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 계산이력
 *  - 취득세 및 양도소득세 계산 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationHistory extends DateEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long calcHistoryId;     // 계산이력ID

    private Long userId;            // 사용자ID
    private String calcType;        // 계산유형
}
