package com.xmonster.howtaxing.repository.product;

import com.xmonster.howtaxing.model.ProductDiscountInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductDiscountInfoRepository extends JpaRepository<ProductDiscountInfo, Long> {

    @Query(value = "SELECT * " +
            "FROM product_discount_info p " +
            "WHERE p.product_id = :productId " +
            "AND (p.product_discount_scope = 'ALWAYS' " +
            "AND p.status = 'ACTIVE') " +
            "ORDER BY product_discount_id DESC", nativeQuery = true)
    List<ProductDiscountInfo> findByProductId(Long productId);
}
