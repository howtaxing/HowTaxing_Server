package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingReservationModifyRequest {
    private Long consultingReservationId;       // 상담예약ID
    private String customerName;                // 고객명
    private String customerPhone;               // 고객전화번호
    private LocalDate reservationDate;          // 예약일자
    private String reservationTime;             // 예약시간
    private String consultingType;              // 상담유형(01:취득세, 02:양도소득세, 03:상속세, 04:재산세)
    private String consultingRequestContent;    // 상담요청내용
}
