package com.xmonster.howtaxing.dto.review;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewRegistRequest {
    private String reviewType;          // (필수) 리뷰유형
    private Integer score;              // (필수) 평점
    private String reviewContents;      // (선택) 리뷰내용
}