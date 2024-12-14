package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingReservationCancelRequest {
    private Long consultingReservationId;   // 상담예약ID
    private String cancelReason;            // 취소사유
}
