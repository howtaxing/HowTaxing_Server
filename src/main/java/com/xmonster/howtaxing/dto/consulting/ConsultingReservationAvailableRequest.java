package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConsultingReservationAvailableRequest {
    private Long consultantId;                  // [필수] 상담자ID
    private LocalDate reservationDate;          // [필수] 예약일자
    private String reservationTime;             // [필수] 예약시간
}
