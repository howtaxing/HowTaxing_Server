package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConsultingReservationEnterDetailRequest {
    private Long consultingReservationId;       // 상담예약ID
    private String consultingType;              // 상담유형(01:취득세, 02:양도소득세, 03:상속세, 04:재산세)
    private String consultingRequestContent;    // 상담요청내용
}
