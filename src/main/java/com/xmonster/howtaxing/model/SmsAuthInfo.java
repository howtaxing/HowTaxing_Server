package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SmsAuthInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long smsAuthId;                 // sms인증ID

    private String socialId;                // 소셜ID
    private String phoneNumber;             // 휴대폰번호
    private String authCode;                // 인증번호
    private LocalDateTime sendDatetime;     // 발송일시
    private LocalDateTime authDatetime;     // 인증일시
}
