package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultantListResponse {
    private Long consultantId;                  // 상담자ID
    private String consultantName;              // 상담자명
    private String jobTitle;                    // 직명
    private String company;                     // 소속회사
    private String location;                    // 지역
    private String consultantIntroduction;      // 상담자소개
    private String thumbImageUrl;               // 썸네일이미지URL
}
