package com.xmonster.howtaxing.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocialUnlinkResponse {
    private Long id;    // 연결 끊기에 성공한 사용자의 회원번호
}
