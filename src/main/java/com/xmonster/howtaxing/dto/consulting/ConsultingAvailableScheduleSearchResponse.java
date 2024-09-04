package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingAvailableScheduleSearchResponse {
    private Long consultantId;                                  // 상담자ID
    private String searchType;                                  // 조회구분(1:상담가능일자조회 2:상담가능시간조회)
    private List<ConsultingAvailableDateResponse> dateList;     // 상담가능일자 리스트(예약일자, 예약가능여부)
    private List<ConsultingAvailableTimeResponse> timeList;     // 상담가능시간 리스트(예약시간, 예약가능여부, 예약상태)

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ConsultingAvailableDateResponse {
        private LocalDate consultingDate;           // 상담일자
        private Boolean isReservationAvailable;     // 예약가능여부
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ConsultingAvailableTimeResponse {
        private String consultingTime;              // 상담시간
        private Integer consultingTimeUnit;         // 상담시간단위(분단위)
        private String reservationStatus;           // 예약상태(1:예약대기 2:예약완료 3:예약불가)
    }
}
