package com.xmonster.howtaxing.utils;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.ConsultantInfo;
import com.xmonster.howtaxing.model.ConsultingReservationInfo;
import com.xmonster.howtaxing.repository.consulting.ConsultantInfoRepository;
import com.xmonster.howtaxing.repository.consulting.ConsultingReservationInfoRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultingReservationUtil {
    private final ConsultingReservationInfoRepository consultingReservationInfoRepository;

    public ConsultingReservationInfo findConsultingReservationInfo(Long consultingReservationId) {
        return consultingReservationInfoRepository.findByConsultingReservationId(consultingReservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_RESERVATION_NOT_FOUND));
    }
}
