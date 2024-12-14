package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.PaymentStatus;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long paymentHistoryId;              // 결제이력ID

    private Long userId;                        // 사용자ID
    private Long productPrice;                  // 상품가격
    private Long productDiscountPrice;          // 상품할인가격
    private Long paymentAmount;                 // 결제금액
    private String method;                      // 결제수단(카드, 가상계좌, 간편결제, 휴대폰, 계좌이체)

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;               // 결제상태(READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED)

    private String paymentKey;                  // 결제키(결제승인, 결제조회, 결제취소 API에 사용)
    private String orderId;                     // 주문번호(6자 이상 64자 이하의 문자열)
    private String orderName;                   // 주문명
    private LocalDateTime tempRequestedAt;      // 임시결제요청일시
    private LocalDateTime requestedAt;          // 결제요청일시
    private LocalDateTime approvedAt;           // 결제승인일시
    private String productIdList;               // 상품ID리스트
    private String productNameList;             // 상품명리스트
    private Long consultingReservationId;       // 상담예약ID
}
