package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingReservationListRequest {
    private Integer page;               // 조회 페이지
    private String consultingStatus;    // 상담진행상태(0:WAITING 1:CANCEL 2:PROGRESS 3:FINISH)
}
