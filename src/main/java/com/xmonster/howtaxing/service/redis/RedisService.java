package com.xmonster.howtaxing.service.redis;

import java.util.Collections;
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

    // 키 개수 가져오기
    public int countKey(Long userId, String type) {
        String pattern = "user:" + userId + ":" + type + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        return keys != null ? keys.size() : 0;
    }

    // hashMap 타입 세션저장
    public void saveHashMap(Long userId, String type, Map<String, String> hashMap) {
        String hashKey = "user:" + userId + ":" + type;

        redisTemplate.opsForHash().putAll(hashKey, hashMap);
        redisTemplate.expire(hashKey, 1, TimeUnit.DAYS);
    }
    public void saveHashMap(Long userId, String type, String id, Map<String, String> hashMap) {
        String hashKey = "user:" + userId + ":" + type + ":" + id;

        redisTemplate.opsForHash().putAll(hashKey, hashMap);
        redisTemplate.expire(hashKey, 1, TimeUnit.DAYS);
    }

    // hashMap 타입 세션조회
    public Map<Object, Object> getHashMap(Long userId, String type, String id) {
        String hashKey = "user:" + userId + ":" + type + ":" + id;
        
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        return entries;
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

    // 특정 공간 value 가져오기
    public String getHashMapValue(Long userId, String type, String id, String key) {
        String hashKey = "user:" + userId + ":" + type + ":" + id;

        // Redis에서 hashMap 조회
        Map<Object, Object> hashMap = redisTemplate.opsForHash().entries(hashKey);
        // 저장된 hashMap에서 value 추출
        String address = (String) hashMap.get(key);

        return address;
    }
}
