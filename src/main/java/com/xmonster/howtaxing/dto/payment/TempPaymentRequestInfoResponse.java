package com.xmonster.howtaxing.dto.payment;

import com.xmonster.howtaxing.type.ConsultingStatus;
import com.xmonster.howtaxing.type.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempPaymentRequestInfoResponse {
    private Long paymentHistoryId;          // 결제이력ID
    private PaymentStatus paymentStatus;    // 결제상태(READY:, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED)
}
