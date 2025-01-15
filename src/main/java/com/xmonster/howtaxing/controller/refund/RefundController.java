package com.xmonster.howtaxing.controller.refund;

import com.xmonster.howtaxing.dto.refund.RefundConsultingApplyInfoRequest;
import com.xmonster.howtaxing.service.refund.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RefundController {
    private final RefundService refundService;

    // (양도소득세)환급상담신청
    @PostMapping("/refund/apply")
    public Object applyRefundConsulting(RefundConsultingApplyInfoRequest refundConsultingApplyInfoRequest) throws Exception {
        log.info(">> [Controller]RefundController applyRefundConsulting - (양도소득세)환급상담신청");
        return refundService.applyRefundConsulting(refundConsultingApplyInfoRequest);
    }
}