package com.ahyakamil.AKCache;

import com.ahyakamil.AKCache.service.RedisCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lettuce.core.api.sync.RedisCommands;
import org.aspectj.lang.ProceedingJoinPoint;

public class AKCacheService {
    public static void setupConnection(String host, int port, String username, String password, int maxTotalPool, int maxIdlePool, int minIdlePool, boolean isUsingPool) {
        RedisCacheService.setupConnection(host, port, username, password, maxTotalPool, maxIdlePool, minIdlePool, isUsingPool);
    }

    public static Object getCache(ProceedingJoinPoint pjp) throws Throwable {
        return RedisCacheService.getCache(pjp);
    }

    public static void renewCache(ProceedingJoinPoint pjp) throws Throwable {
        RedisCacheService.renewCache(pjp);
    }

    public static void deleteCache(ProceedingJoinPoint pjp) throws Throwable {
        RedisCacheService.deleteCache(pjp);
    }

    public static RedisCommands<String, String> nativeQuery() throws JsonProcessingException {
        return RedisCacheService.nativeQuery();
    }
}