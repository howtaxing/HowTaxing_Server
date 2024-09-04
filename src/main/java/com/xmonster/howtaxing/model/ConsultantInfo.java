package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ConsultantInfo extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long consultantId;                  // 상담자ID

    private String consultantName;              // 상담자명
    private String consultantType;              // 상담자유형
    private String consultingAvailableType;     // 상담가능유형
    private String consultantExplain;           // 상담자설명
    private String consultantProfile;           // 상담자프로필
    private Boolean isConsultingAvailable;      // 상담가능여부
    private Boolean isDeleted;                  // 삭제여부
}
