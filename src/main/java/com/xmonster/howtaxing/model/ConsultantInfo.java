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
    private Long consultantId;                      // 상담자ID

    private String consultantName;                  // 상담자명
    private String jobTitle;                        // 직명
    private String company;                         // 소속회사(ex:JS세무회게)
    private String qualification;                   // 자격명(ex:세무사, 공인중개사 전문가)
    private String location;                        // 지역(ex:서울특별시 송파구)
    private String consultingType;                  // 상담유형(콤마(,)로 구분 - (01:취득세 02:양도소득세 03:상속세 04:재산세))
    private String consultantIntroduction;          // 상담자소개
    private String specialtyContents;               // 전문분야(콤마(,)로 구분)
    private String majorExperienceContents;         // 주요경력(콤마(,)로 구분)
    private String profileImageUrl;                 // 프로필이미지URL
    private String thumbImageUrl;                   // 썸네일이미지URL
    private Boolean isConsultingAvailable;          // 상담가능여부
    private String consultingUnavailableReason;     // 상담불가사유
}
