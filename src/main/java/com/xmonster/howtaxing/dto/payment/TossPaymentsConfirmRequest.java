package com.xmonster.howtaxing.dto.payment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TossPaymentsConfirmRequest {
    private String paymentKey;      // [필수] 결제키(결제승인, 결제조회, 결제취소 API에 사용)
    private String orderId;         // [필수] 주문번호(6자 이상 64자 이하의 문자열)
    private String amount;          // [필수] 결제금액
}
