package com.xmonster.howtaxing.dto.payment;

import com.xmonster.howtaxing.type.ConsultingStatus;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PaymentDetailResponse {
    private Long paymentHistoryId;              // 결제이력ID
    private String consultantName;              // 상담자명
    private String thumbImageUrl;               // (상담자)썸네일이미지URL
    
    private String approvedDatetime;            // 결제승인일시
    private Long productPrice;                  // 상품가격
    private Long productDiscountPrice;          // 할인가격
    private Long paymentAmount;                 // 결제금액
    private String method;                      // 결제수단(카드, 가상계좌, 간편결제, 휴대폰, 계좌이체)
}
