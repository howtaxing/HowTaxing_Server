package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultantDetailResponse {
    private Long consultantId;                      // 상담자ID
    private String consultantName;                  // 상담자명(ex:이민정음)
    private String jobTitle;                        // 직명(ex:세무사)
    private String company;                         // 소속회사(ex:JS세무회게)
    private String qualification;                   // 자격명(ex:세무사, 공인중개사 전문가)
    private String location;                        // 지역(ex:서울특별시 송파구)
    private String consultingType;                  // 상담유형(콤마(,)로 구분 - (01:취득세 02:양도소득세 03:상속세 04:재산세))
    private String consultantIntroduction;          // 상담자소개
    private List<String> specialtyList;             // 전문분야 내용 목록
    private List<String> majorExperienceList;       // 주요경력 내용 목록
    private String profileImageUrl;                 // 프로필이미지URL
}