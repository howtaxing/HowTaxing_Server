package com.xmonster.howtaxing.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class SmsMessageBodyRequest {
    private String type;
    private String contentType;
    private String countryCode;
    private String from;
    private String subject;
    private String content;
    private List<Message> messages;
    private List<File> files;
    private String reserveTime;
    private String reserveTimeZone;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Message {
        public String to;
        public String subject;
        public String content;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class File {
        public String fileId;
    }
}
