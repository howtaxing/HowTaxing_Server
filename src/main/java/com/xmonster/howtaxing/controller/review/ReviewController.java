package com.xmonster.howtaxing.controller.review;

import com.xmonster.howtaxing.service.review.ReviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService){
        this.reviewService = reviewService;
    }

    // 리뷰 등록
    @PostMapping("/review/registReview")
    public Map<String, Object> registReview(@RequestBody Map<String, Object> requestMap){
        return reviewService.registReview(requestMap);
    }
}
