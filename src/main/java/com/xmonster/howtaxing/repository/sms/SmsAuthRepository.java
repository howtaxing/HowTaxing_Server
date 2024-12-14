package com.xmonster.howtaxing.repository.sms;

import com.xmonster.howtaxing.model.SmsAuthInfo;
import com.xmonster.howtaxing.type.AuthType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SmsAuthRepository extends JpaRepository<SmsAuthInfo, Long> {
    //SmsAuthInfo findLastByPhoneNumber(String phoneNumber);

    // 인증정보 가져오기(가장 마지막 발송된 인증번호 추출 목적)
    SmsAuthInfo findTopByPhoneNumberAndAuthTypeOrderBySendDatetimeDesc(String phoneNumber, AuthType authType);

    // 당일 인증번호 발송 횟수 조회(인증유형 별)
    Long countByPhoneNumberAndAuthTypeAndSendDatetimeBetween(String phoneNumber, AuthType authType, LocalDateTime startOfDay, LocalDateTime endOfDay);

    // 인증정보 가져오기(인증키 비교 및 사용 처리 목적)
    SmsAuthInfo findTopByAuthKeyOrderByAuthDatetimeDesc(String authKey);
}
