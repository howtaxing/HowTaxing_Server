package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 건축물대장정보
 *  - 하이픈을 통해 청약홈에서 보유주택정보를 조회한 데이터 중 건축물대장정보를 저장
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HouseBuildingRegisterInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long buildingRegisterId;            // 건축물대장ID

    private Long searchId;                      // 조회ID
    private String name;                        // 성명
    private String address;                     // 주소
    private String area;                        // 면적(m^2)
    private String approvalDate;                // 사용승인일
    private String reasonChangeOwnership;       // 소유권변동사유
    private String ownershipChangeDate;         // 소유권변동일
    private String publicationBaseDate;         // 공시기준일
    private String publishedPrice;              // 공시가격(천원)
    private String baseDate;                    // 기준일자
}