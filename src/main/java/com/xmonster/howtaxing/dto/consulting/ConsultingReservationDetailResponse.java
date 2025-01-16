package com.xmonster.howtaxing.dto.consulting;

import com.xmonster.howtaxing.dto.calculation.CalculationBuyHouseResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellHouseResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse;
import com.xmonster.howtaxing.type.ConsultingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConsultingReservationDetailResponse {
    private Long consultingReservationId;           // 상담예약ID
    private String consultantName;                  // 상담자명
    private String profileImageUrl;                 // 상담자프로필이미지URL

    private String consultingType;                  // 상담유형(콤마(,)로 구분 - (01:취득세 02:양도소득세 03:상속세 04:재산세))
    private LocalDate reservationDate;              // 예약일자
    private String reservationStartTime;            // 예약시작시간
    private String reservationEndTime;              // 예약종료시간
    private ConsultingStatus consultingStatus;      // 상담진행상태(PAYMENT_READY:결제대기, PAYMENT_COMPLETE:결제완료, WAITING:상담대기, CANCEL:상담취소, PROGRESS:상담중, FINISH:상담종료)
    private String consultingInflowPath;            // 상담유입경로(00:일반 01:취득세계산 02:양도소득세계산)
    private Long paymentAmount;                     // 결제금액
    private String consultingRequestContent;        // 상담요청내용

    private String paymentCompleteDatetime;         // 결제완료일시
    private String consultingCancelDatetime;        // 상담취소일시
    private String consultingStartDatetime;         // 상담시작일시
    private String consultingEndDatetime;           // 상담종료일시

    private CalculationBuyHouseResponse calculationBuyHouseResponse;        // (취득세 계산대상)취득주택 정보
    private CalculationSellHouseResponse calculationSellHouseResponse;      // (양도소득세 계산대상)양도주택 정보

    private CalculationBuyResultResponse calculationBuyResultResponse;      // 취득세 계산결과
    private CalculationSellResultResponse calculationSellResultResponse;    // 양도소득세 계산결과
}