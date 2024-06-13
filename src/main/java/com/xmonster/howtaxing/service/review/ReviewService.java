package com.xmonster.howtaxing.service.review;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewService {

    // 리뷰 등록
    public Map<String, Object> registReview(Map<String, Object> requestMap){

        Map<String, Object> resultMap = new HashMap<String, Object>();

        if(requestMap != null && !requestMap.isEmpty()){
            resultMap.put("isError", "false");
        }else{
            resultMap.put("isError", "true");
            resultMap.put("errCode", "4001");
            resultMap.put("errMsg", "작성한 리뷰가 등록되지 않았습니다.");
        }

        return resultMap;
    }
}
