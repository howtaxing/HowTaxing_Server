package com.xmonster.howtaxing.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class SocialUnlinkRequest {
    private String targetIdType;    // 회원번호 종류(user_id로 고정)
    private Long targetId;          // 연결을 끊을 사용자의 회원번호
}
