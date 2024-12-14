package com.xmonster.howtaxing.utils;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.ConsultantInfo;
import com.xmonster.howtaxing.model.PaymentHistory;
import com.xmonster.howtaxing.repository.consulting.ConsultantInfoRepository;
import com.xmonster.howtaxing.repository.payment.PaymentHistoryRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentUtil {
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentHistory findPaymentHistory(Long paymentHistoryId){
        return paymentHistoryRepository.findByPaymentHistoryId(paymentHistoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));
    }

    public PaymentHistory findPaymentHistoryByConsultingReservationId(Long consultingReservationId){
        return paymentHistoryRepository.findByConsultingReservationId(consultingReservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));
    }
}
