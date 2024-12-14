package com.xmonster.howtaxing.controller.payment;

import com.xmonster.howtaxing.dto.payment.PaymentConfirmRequest;
import com.xmonster.howtaxing.dto.payment.TempPaymentRequestInfoRequest;
import com.xmonster.howtaxing.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    // 결제요청 정보 임시저장
    @PostMapping("/payment/saveTemp")
    public Object saveTempPaymentRequestInfo(@RequestBody TempPaymentRequestInfoRequest tempPaymentRequestInfoRequest) throws Exception {
        log.info(">> [Controller]PaymentController saveTempPaymentRequestInfo - 결제요청 정보 임시저장");
        return paymentService.saveTempPaymentRequestInfo(tempPaymentRequestInfoRequest);
    }
    
    // 결제 승인
    @PostMapping("/payment/confirm")
    public Object confirmPayment(@RequestBody PaymentConfirmRequest paymentConfirmRequest) throws Exception {
        log.info(">> [Controller]PaymentController confirmPayment - 결제 승인");
        return paymentService.confirmPayment(paymentConfirmRequest);
    }

    // 결제목록 조회
    @GetMapping("/payment/list")
    public Object getPaymentList() throws Exception {
        log.info(">> [Controller]PaymentController getPaymentList - 결제목록 조회");
        return paymentService.getPaymentList();
    }

    // 결제상세 조회
    @GetMapping("/payment/detail")
    public Object getPaymentDetail(@RequestParam Long paymentHistoryId) throws Exception {
        log.info(">> [Controller]PaymentController getPaymentDetail - 결제상세 조회");
        return paymentService.getPaymentDetail(paymentHistoryId);
    }
}
