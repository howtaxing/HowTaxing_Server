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

    // hashMap 타입 세션저장
    public void saveHashMap(Long userId, String type, Map<String, String> hashMap) {
        String hashKey = "user:" + userId + ":" + type;

        redisTemplate.opsForHash().putAll(hashKey, hashMap);
        redisTemplate.expire(hashKey, 1, TimeUnit.DAYS);
    }
    public void saveHashMap(Long userId, String type, int id, Map<String, String> hashMap) {
        String hashKey = "user:" + userId + ":" + type + ":" + id;

        redisTemplate.opsForHash().putAll(hashKey, hashMap);
        redisTemplate.expire(hashKey, 1, TimeUnit.DAYS);
    }

    // hashMap 타입 일괄삭제
    public void deleteHashMap(Long userId, String type) {
        String hashKey = "user:" + userId + ":" + type + ":*";
        Set<String> keys = redisTemplate.keys(hashKey);

        redisTemplate.delete(keys);
    }

    // 사용자정보 삭제
    public void deleteForUser(Long userId) {
        String hashKey = "user:" + userId + ":userInfo";

        redisTemplate.delete(hashKey);
    }
}
