package com.xmonster.howtaxing.dto.payment;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PaymentListResponse {
    private Long paymentHistoryId;              // 결제이력ID
    private String approvedDatetime;            // 결제승인일시
    private Long paymentAmount;                 // 결제금액
    private String consultantName;              // 상담자명
    private String thumbImageUrl;               // (상담자)썸네일이미지URL
}
