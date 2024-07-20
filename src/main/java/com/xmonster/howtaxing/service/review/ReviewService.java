package com.xmonster.howtaxing.service.review;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.review.ReviewRegistRequest;
import com.xmonster.howtaxing.model.Review;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.review.ReviewRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.ReviewType;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;

    private final UserUtil userUtil;

    // 리뷰 등록
    public Object registReview(ReviewRegistRequest reviewRegistRequest){
        log.info(">> [Service]ReviewService registReview - 리뷰 등록");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        // 리뷰등록 요청값 유효성 검증
        validationCheckForRegistReview(reviewRegistRequest);

        try{
            reviewRepository.saveAndFlush(
                    Review.builder()
                            .userId(findUser.getId())
                            .reviewType(ReviewType.valueOf(reviewRegistRequest.getReviewType().toUpperCase()))
                            .score(reviewRegistRequest.getScore())
                            .reviewContents(reviewRegistRequest.getReviewContents())
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "DB 저장 오류");
        }

        return ApiResponse.success(Map.of("result", "리뷰 등록이 완료되었습니다."));
    }

    private void validationCheckForRegistReview(ReviewRegistRequest reviewRegistRequest){
        if(reviewRegistRequest == null) throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "리뷰등록을 위한 요청값이 존재하지 않습니다.");

        String reviewType = reviewRegistRequest.getReviewType();
        Integer score = reviewRegistRequest.getScore();
        String reviewContents = reviewRegistRequest.getReviewContents();

        if(StringUtils.isBlank(reviewType)){
            throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "(리뷰등록)리뷰유형이 입력되지 않았습니다.");
        }else{
            if(!EnumUtils.isValidEnum(ReviewType.class, reviewType.toUpperCase())){
                throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "(리뷰등록)리뷰유형이 올바르지 않습니다.(COMMON, BUY, SELL, CONSULT)");
            }
        }
        
        if(score == null){
            throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "(리뷰등록)평점이 입력되지 않았습니다.");
        }else{
            if(score < 0 || score > 5){
                throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "(리뷰등록)평점 값의 범위를 벗어났습니다.(0<=score<=5)");
            }
        }

        if(!StringUtils.isBlank(reviewContents)){
            if(reviewContents.length() > 1000){
                throw new CustomException(ErrorCode.REVIEW_REGIST_ERROR, "(리뷰등록)리뷰내용이 1000바이트를 초과하였습니다.");
            }
        }
    }
}
