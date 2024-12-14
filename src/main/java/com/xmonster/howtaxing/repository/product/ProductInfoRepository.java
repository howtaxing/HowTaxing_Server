package com.xmonster.howtaxing.repository.product;

import com.xmonster.howtaxing.model.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, Long> {

    List<ProductInfo> findByConsultantId(Long consultantId);

    @Query(value = "SELECT * " +
                    "FROM product_info p " +
                    "WHERE p.product_scope = 'COMMON' " +
                        "AND p.product_type = 'CONSULTING' " +
                        "AND p.status = 'ACTIVE' " +
                    "ORDER BY product_id DESC", nativeQuery = true)
    List<ProductInfo> findCommonProductInfo();
}
