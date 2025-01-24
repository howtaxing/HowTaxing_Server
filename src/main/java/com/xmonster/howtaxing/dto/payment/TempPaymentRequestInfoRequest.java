package com.xmonster.howtaxing.dto.payment;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TempPaymentRequestInfoRequest {
    private Long consultantId;                  // [필수] 상담자ID
    private String customerName;                // [필수] 고객명
    private String customerPhone;               // [필수] 고객전화번호
    private LocalDate reservationDate;          // [필수] 예약일자
    private String reservationTime;             // [필수] 예약시간
    private String consultingType;              // [선택] 상담유형(01:취득세, 02:양도소득세, 03:상속세, 04:재산세)
    private String consultingInflowPath;        // [필수] 상담유입경로(00:일반, 01:취득세 계산, 02:양도소득세 계산)
    private Long calcHistoryId;                 // [선택] 계산이력ID
    private String orderId;                     // [필수] 주문번호(6자 이상 64자 이하의 문자열)
    private String orderName;                   // [선택] 주문명(상품명 productName 과 동일, 추후 변경 예정)
    private Long productPrice;                  // [선택] 상품가격
    private Long productDiscountPrice;          // [선택] 상품할인가격
    private Long paymentAmount;                 // [필수] 결제금액
    private Long productId;                     // [선택] 상품ID
    private String productName;                 // [선택] 상품명
}
