package com.xmonster.howtaxing.utils;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.ConsultantInfo;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.repository.consulting.ConsultantInfoRepository;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConsultantUtil {
    private final ConsultantInfoRepository consultantInfoRepository;

    public ConsultantInfo findSelectedConsultantInfo(Long consultantId) {
        return consultantInfoRepository.findByConsultantId(consultantId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_CONSULTANT_NOT_FOUND));
    }

    public String findConsultantName(Long consultantId) {
        return findSelectedConsultantInfo(consultantId).getConsultantName();
    }
}
