package com.xmonster.howtaxing.dto.consulting;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConsultingReservationApplyForFreeRequest {
    private Long consultantId;                  // [필수] 상담자ID
    private String customerName;                // [필수] 고객명
    private String customerPhone;               // [필수] 고객전화번호
    private LocalDate reservationDate;          // [필수] 예약일자
    private String reservationTime;             // [필수] 예약시간
    private String consultingInflowPath;        // [필수] 상담유입경로(00:일반, 01:취득세 계산, 02:양도소득세 계산)
    private Long calcHistoryId;                 // [선택] 계산이력ID
    private String consultingType;              // [선택] 상담유형(01:취득세, 02:양도소득세, 03:상속세, 04:재산세)
    private String consultingRequestContent;    // [필수] 상담요청내용
}