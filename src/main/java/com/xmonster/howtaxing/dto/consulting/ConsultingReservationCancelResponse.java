package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingReservationCancelResponse {
    private Boolean isCancelComplete;       // 취소완료여부
    private String consultantName;          // 상담자명
    private LocalDate reservationDate;      // 예약일자
    private String reservationStartTime;    // 예약시작시간
    private String reservationEndTime;      // 예약종료시간
}
