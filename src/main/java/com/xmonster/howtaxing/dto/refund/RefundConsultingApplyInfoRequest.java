package com.xmonster.howtaxing.dto.refund;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RefundConsultingApplyInfoRequest {
    private String customerPhone;               // 고객전화번호
    private Boolean isRefundAvailable;          // 환급대상여부
}