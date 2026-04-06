package com.safechat.userservice.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CachedService {
    
    private final RedisTemplate<String,Object> redisTemplate;

    public CachedService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> T getFromCache(String cacheKey, Class<T> clazz) {
        Object cachedResponse = redisTemplate.opsForValue().get(cacheKey);
        return clazz.cast(cachedResponse);
    }

    public void saveResponse(String cacheKey, Object response, Duration Ttl) {
        redisTemplate.opsForValue().set(cacheKey, response, Ttl);
    }

    public void deleteCacheByKeyPattern(String cacheKey) {
        Set<String> keys = redisTemplate.keys(cacheKey);// Delete by pattern (with wildcards)
        redisTemplate.delete(keys);
    }

    public void deleteCacheByKeyPatternSet(Set<String> patterns) {
        Set<String> allKeys = new HashSet<>();

        for (String pattern : patterns) {
            Set<String> matchedKeys = redisTemplate.keys(pattern);
            // matchedKeys is rarely null in practice
            allKeys.addAll(matchedKeys); // Risk: NPE if matchedKeys is null
        }

        redisTemplate.delete(allKeys);
    }

    public void deleteCacheByKeySet(Set<String> cacheKeys) {
        redisTemplate.delete(cacheKeys);
    }

    public void deleteCacheByKey(String cacheKey) {
        redisTemplate.delete(cacheKey);
    }
}