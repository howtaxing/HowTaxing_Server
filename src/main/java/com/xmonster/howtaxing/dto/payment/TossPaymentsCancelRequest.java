package com.xmonster.howtaxing.dto.payment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TossPaymentsCancelRequest {
    private String cancelReason;                        // [필수] 취소이유(최대 길이 200자)
    private Long cancelAmount;                          // [선택] 취소금액(미입력시 전액 취소 처리)
    private RefundReceiveAccount refundReceiveAccount;  // [선택] 결제취소 후 환불 계좌 정보
    private Long taxFreeAmount;                         // [선택] 취소금액 중 면세금액(기본값 0)
    private String currency;                            // [선택] 취소통화(PayPal 해외간편결제 부분취소 시 필수, PayPal은 USD 사용)
    private Long refundableAmount;                      // [선택] 환불가능금액(deprecated)

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RefundReceiveAccount {
        private String bank;                            // [필수] 취소금액 환불계좌 은행 코드
        private String accountNumber;                   // [필수] 환불 계좌번호
        private String holderName;                      // [필수] 예금주
    }
}
