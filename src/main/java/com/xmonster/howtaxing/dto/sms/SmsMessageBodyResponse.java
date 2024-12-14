package com.xmonster.howtaxing.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 네이버 클라우드 플랫폼 메시지 발송
 * https://api.ncloud-docs.com/docs/ai-application-service-sens-smsv2#APIHeader
 * v1.0 : 최초 작성 / 2024.11.14 김웅태
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsMessageBodyResponse {
    private String requestId;               // 요청아이디
    private String requestTime;             // 요청시간
    private String statusCode;              // 요청상태코드(202:성공, 그 외:실패, HTTP Status 규격을 따름)
    private String statusName;              // 요청상태명(success:성공, fail:실패)
}
