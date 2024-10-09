package com.xmonster.howtaxing.dto.main;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MainPopupInfoResponse {
    private Long mainPopupId;       // 메인팝업ID
    private String targetUrl;       // 타겟URL
    private String imageUrl;        // 이미지URL
    private Boolean isPost;         // 게시여부
}
