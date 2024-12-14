package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.CommonStatus;
import com.xmonster.howtaxing.type.ProductDiscountScope;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductDiscountInfo extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long productDiscountId;                     // 상품할인ID

    private Long productId;                             // 상품ID

    @Enumerated(EnumType.STRING)
    private ProductDiscountScope productDiscountScope;  // 상품범위(ALWAYS:항상, PERIOD:기간)

    private LocalDateTime productDiscountStartDatetime; // 상품할인시작일시
    private LocalDateTime productDiscountEndDatetime;   // 상품할인종료일시
    private Long productDiscountPrice;                  // 상품할인가격

    @Enumerated(EnumType.STRING)
    private CommonStatus status;                        // 상태(ACTIVE:사용, INACTIVE:미사용, DELETED:삭제)
}