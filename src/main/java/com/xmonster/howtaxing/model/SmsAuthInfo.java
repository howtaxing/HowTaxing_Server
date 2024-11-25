package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.AuthType;
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
    private Long smsAuthId;                     // sms인증ID

    private String phoneNumber;                 // 휴대폰번호
    private String socialId;                    // 소셜ID

    @Enumerated(EnumType.STRING)
    private AuthType authType;                  // 인증유형

    private String authCode;                    // 인증번호
    private LocalDateTime sendDatetime;         // 발송일시
    private LocalDateTime authDatetime;         // 인증일시
    private String authKey;                     // 인증키(인증일시로부터 유효기간 24시간)
    private Boolean isAuthKeyUsed;              // 인증키사용여부
}
