package com.xmonster.howtaxing.controller.review;

import com.xmonster.howtaxing.dto.review.ReviewRegistRequest;
import com.xmonster.howtaxing.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 등록
    @PostMapping("/review/registReview")
    public Object registReview(@RequestBody ReviewRegistRequest reviewRegistRequest) throws Exception {
        log.info(">> [Controller]ReviewController registReview - 리뷰 등록");
        return reviewService.registReview(reviewRegistRequest);
    }
}