package com.xmonster.howtaxing.dto.payment;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PaymentConfirmRequest {
    private Long paymentHistoryId;              // [필수] 결제이력ID
    private String paymentKey;                  // [필수] 결제키(결제승인, 결제조회, 결제취소 API에 사용)
    private String orderId;                     // [필수] 주문번호(6자 이상 64자 이하의 문자열)
    private Long paymentAmount;                 // [필수] 결제금액
}
