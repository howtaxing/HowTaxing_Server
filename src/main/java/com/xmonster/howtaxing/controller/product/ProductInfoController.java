package com.xmonster.howtaxing.controller.product;

import com.xmonster.howtaxing.service.product.ProductInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductInfoController {
    private final ProductInfoService productInfoService;

    // 상품 정보 조회
    @GetMapping("/product/productInfo")
    public Object getProductInfo(@RequestParam Long consultantId) throws Exception {
        log.info(">> [Controller]ProductInfoController getProductInfo - 상품 정보 조회");
        return productInfoService.getProductInfo(consultantId);
    }
}
