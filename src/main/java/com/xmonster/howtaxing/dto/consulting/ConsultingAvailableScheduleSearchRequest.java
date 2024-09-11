package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingAvailableScheduleSearchRequest {
    private Long consultantId;              // 상담자ID
    private String searchType;              // 조회구분(1:상담가능일자조회 2:상담가능시간조회)
    private String searchYearAndMonth;      // 조회년월(ex:2024-09)
    private String searchDate;              // 조회일(ex:12)
}
