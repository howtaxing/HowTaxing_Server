package com.xmonster.howtaxing.dto.payment;

import com.xmonster.howtaxing.type.ConsultingStatus;
import com.xmonster.howtaxing.type.PaymentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PaymentConfirmResponse {
    private Long paymentHistoryId;              // 결제이력ID
    private PaymentStatus paymentStatus;        // 결제상태(READY:, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED)
    private LocalDateTime requestedAt;          // 결제요청일시
    private LocalDateTime approvedAt;           // 결제승인일시
    private Long consultingReservationId;       // 상담예약ID
    private ConsultingStatus consultingStatus;  // 상담진행상태(PAYMENT_READY:결제대기, PAYMENT_COMPLETE:결제완료, WAITING:상담대기, CANCEL:상담취소, PROGRESS:상담중, FINISH:상담종료)
}
