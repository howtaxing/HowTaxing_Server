package com.xmonster.howtaxing.service.product;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.product.ProductInfoResponse;
import com.xmonster.howtaxing.model.ProductDiscountInfo;
import com.xmonster.howtaxing.model.ProductInfo;
import com.xmonster.howtaxing.repository.product.ProductDiscountInfoRepository;
import com.xmonster.howtaxing.repository.product.ProductInfoRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductInfoService {
    private final ProductInfoRepository productInfoRepository;
    private final ProductDiscountInfoRepository productDiscountInfoRepository;

    // 상품 정보 조회
    public Object getProductInfo(Long consultantId) throws Exception {
        log.info(">> [Service]ProductInfoService getProductInfoList - 상품 정보 목록 조회");

        if(consultantId == null){
            throw new CustomException(ErrorCode.PRODUCT_INFO_INPUT_ERROR, "상담자 ID가 입력되지 않았어요.");
        }

        List<ProductInfo> productInfoList = productInfoRepository.findByConsultantId(consultantId);

        if(productInfoList == null || productInfoList.isEmpty()){
            productInfoList = productInfoRepository.findCommonProductInfo();
        }

        if(productInfoList == null || productInfoList.isEmpty()){
            throw new CustomException(ErrorCode.PRODUCT_INFO_OUTPUT_ERROR);
        }

        long productPrice = 0;              // 상품가격
        long productDiscountPrice = 0;      // 할인가격
        long paymentAmount = 0;             // 결제금액

        ProductInfo productInfo = productInfoList.get(0);
        productPrice = productInfo.getProductPrice();

        long productId = productInfo.getProductId();
        List<ProductDiscountInfo> productDiscountInfoList = productDiscountInfoRepository.findByProductId(productId);
        if(productDiscountInfoList != null && !productDiscountInfoList.isEmpty()){
            ProductDiscountInfo productDiscountInfo = productDiscountInfoList.get(0);
            productDiscountPrice = productDiscountInfo.getProductDiscountPrice();
        }

        paymentAmount = productPrice - productDiscountPrice;
        if(paymentAmount < 0) paymentAmount = 0;

        return ApiResponse.success(
                ProductInfoResponse.builder()
                        .productId(productId)
                        .productScope(productInfo.getProductScope())
                        .consultantId(consultantId)
                        .productType(productInfo.getProductType())
                        .productName(productInfo.getProductName())
                        .productDetail(productInfo.getProductDetail())
                        .productPrice(productPrice)
                        .productDiscountPrice(productDiscountPrice)
                        .paymentAmount(paymentAmount)
                        .remark(productInfo.getRemark())
                        .build());
    }
}
