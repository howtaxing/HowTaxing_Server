package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.CommonStatus;
import com.xmonster.howtaxing.type.ProductScope;
import com.xmonster.howtaxing.type.ProductType;
import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductInfo extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long productId;                     // 상품ID

    @Enumerated(EnumType.STRING)
    private ProductScope productScope;          // 상품범위(COMMON:공통, CUSTOM:개인)

    private Long consultantId;                  // 상담자ID

    @Enumerated(EnumType.STRING)
    private ProductType productType;            // 상품유형(CONSULTING:상담, TAX_FILING_ASSISTANT:세무신고대리)

    private String productName;                 // 상품명
    private String productDetail;               // 상품상세
    private Long productPrice;                  // 상품가격(원)

    @Enumerated(EnumType.STRING)
    private CommonStatus status;                // 상태(ACTIVE:사용, INACTIVE:미사용, DELETED:삭제)

    private String remark;                      // 비고
}
