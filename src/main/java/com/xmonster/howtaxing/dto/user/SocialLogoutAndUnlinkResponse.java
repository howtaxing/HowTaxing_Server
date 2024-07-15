package com.xmonster.howtaxing.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocialLogoutAndUnlinkResponse {
    // (카카오)
    private Long id;                // 연결 끊기에 성공한 사용자의 회원번호

    // (네이버)
    private String access_token;     // 삭제 처리된 접근 토큰
    private String result;          // 처리결과(success)
}
