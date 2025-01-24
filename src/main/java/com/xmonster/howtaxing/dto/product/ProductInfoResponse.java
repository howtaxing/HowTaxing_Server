package com.xmonster.howtaxing.dto.product;

import com.xmonster.howtaxing.type.ProductScope;
import com.xmonster.howtaxing.type.ProductType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductInfoResponse {
    private Long productId;                 // 상품ID
    private ProductScope productScope;      // 상품범위(COMMON:공통, CUSTOM:개인)
    private Long consultantId;              // 상담자ID
    private ProductType productType;        // 상품유형(CONSULTING:상담, TAX_FILING_ASSISTANT:세무신고대리)
    private String productName;             // 상품명
    private String productDetail;           // 상품상세
    private Long productPrice;              // 상품가격
    private Long productDiscountPrice;      // 할인가격
    private Long paymentAmount;             // 결제금액
    private String remark;                  // 비고
}
