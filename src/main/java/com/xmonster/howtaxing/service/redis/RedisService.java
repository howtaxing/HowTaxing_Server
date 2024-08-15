package com.xmonster.howtaxing.service.redis;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 재산세정보 저장
    public void savePropertyInfo(Long userId, int houseNo, Map<String, String> propertyInfo) {
        String hashKey = "user:" + userId + ":property:" + houseNo;

        redisTemplate.opsForHash().putAll(hashKey, propertyInfo);
        redisTemplate.expire(hashKey, 1, TimeUnit.DAYS);
    }

    // 사용자 재산세정보 일괄삭제
    public void deletePropertyInfo(Long userId) {
        String hashKey = "user:" + userId + ":property:*";
        Set<String> keys = redisTemplate.keys(hashKey);

        redisTemplate.delete(keys);
    }
}
