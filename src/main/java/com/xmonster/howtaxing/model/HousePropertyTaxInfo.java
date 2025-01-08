package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 재산세정보
 *  - 하이픈을 통해 청약홈에서 보유주택정보를 조회한 데이터 중 재산세정보를 저장
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HousePropertyTaxInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long propertyTaxId;                 // 재산세ID

    private Long searchId;                      // 조회ID
    private String name;                        // 성명
    private String address;                     // 주소
    private String area;                        // 전용면적(m^2)
    private String acquisitionDate;             // 취득일자
    private String baseDate;                    // 기준일자
}