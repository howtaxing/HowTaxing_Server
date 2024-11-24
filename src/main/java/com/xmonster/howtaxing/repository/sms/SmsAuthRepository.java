package com.xmonster.howtaxing.repository.sms;

import com.xmonster.howtaxing.model.SmsAuthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SmsAuthRepository extends JpaRepository<SmsAuthInfo, Long> {
    //SmsAuthInfo findLastByPhoneNumber(String phoneNumber);

    // 인증정보 가져오기(가장 마지막 발송된 인증번호 추출 목적)
    SmsAuthInfo findTopByPhoneNumberOrderBySendDatetimeDesc(String phoneNumber);

    // 당일 인증번호 발송 횟수 조회
    Long countByPhoneNumberAndSendDatetimeBetween(String phoneNumber, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
