package com.xmonster.howtaxing.feign.tosspayments;

import com.xmonster.howtaxing.config.FeignConfiguration;
import com.xmonster.howtaxing.dto.payment.TossPaymentsCancelRequest;
import com.xmonster.howtaxing.dto.payment.TossPaymentsConfirmRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "paymentsConfirm", url="https://api.tosspayments.com/v1", configuration = {FeignConfiguration.class})
public interface PaymentsConfirmApi {
    // 결제 승인
    @PostMapping("/payments/confirm")
    ResponseEntity<String> confirmPayment(@RequestHeader Map<String, Object> requestHeader,
                                          @RequestBody TossPaymentsConfirmRequest requestBody);

    // 결제 취소
    @PostMapping("/payments/{paymentKey}/cancel")
    ResponseEntity<String> cancelPayment(@PathVariable("paymentKey") String paymentKey,
                                          @RequestHeader Map<String, Object> requestHeader,
                                          @RequestBody TossPaymentsCancelRequest requestBody);
}
