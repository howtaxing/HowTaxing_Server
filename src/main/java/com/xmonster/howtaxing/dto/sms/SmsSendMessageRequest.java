package com.xmonster.howtaxing.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsSendMessageRequest {
    private String phoneNumber;     // [필수] 전화번호(휴대폰번호)
    private String messageContent;  // [필수] 메시지 내용
}
