package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingReservationListResponse {
    private int listCnt;
    private List<ConsultingReservationSimpleResponse> list;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ConsultingReservationSimpleResponse {
        private Long consultingReservationId;   // 상담예약ID
        private String consultantName;          // 상담자명
        private String consultingType;          // 상담유형(콤마(,)로 구분 - (01:취득세 02:양도소득세 03:상속세 04:재산세))
        private LocalDate reservationDate;      // 예약일자
        private String reservationStartTime;    // 예약시작시간
        private String reservationEndTime;      // 예약종료시간
        private String consultingStatus;        // 상담진행상태(0:WAITING 1:CANCEL 2:PROGRESS 3:FINISH)
    }
}