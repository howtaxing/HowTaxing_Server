package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
/**
 * 계산보유주택이력
 *  - 취득세 및 양도소득세 계산 시 보유주택 이력
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculationOwnHouseHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long ownHouseHistoryId;     // 보유주택이력ID

    private Long calcHistoryId;         // 계산이력ID
    private Long ownHouseCnt;        // 보유주택수
    private Boolean hasOwnHouseDetail;  // 보유주택상세(정보)존재여부
}
