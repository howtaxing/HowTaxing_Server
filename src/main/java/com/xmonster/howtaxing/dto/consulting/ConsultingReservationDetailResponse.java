package com.xmonster.howtaxing.dto.consulting;

import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsultingReservationDetailResponse {
    private Long consultingReservationId;   // 상담예약ID
    private String consultantName;          // 상담자명
    private String consultingType;          // 상담유형(콤마(,)로 구분 - (01:취득세 02:양도소득세 03:상속세 04:재산세))
    private LocalDate reservationDate;      // 예약일자
    private String reservationStartTime;    // 예약시작시간
    private String reservationEndTime;      // 예약종료시간
    private String consultingStatus;        // 상담진행상태(0:WAITING 1:CANCEL 2:PROGRESS 3:FINISH)
    private String consultingInflowPath;    // 상담유입경로(00:일반 01:취득세계산 02:양도소득세계산)

    private CalculationBuyResultResponse calculationBuyResultResponse;      // 취득세 계산결과
    private CalculationSellResultResponse calculationSellResultResponse;    // 양도소득세 계산결과
}