package com.ahyakamil.AKCache;

import com.ahyakamil.AKCache.service.RedisCacheService;
import org.aspectj.lang.ProceedingJoinPoint;

public class AKCacheSetup {
    public static void setupConnection(String host, int port, String username, String password, int maxTotalPool, int maxIdlePool, int minIdlePool, boolean isUsingPool) {
        RedisCacheService.setupConnection(host, port, username, password, maxTotalPool, maxIdlePool, minIdlePool, isUsingPool);
    }

    public static Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        return RedisCacheService.setListener(pjp);
    }

    public static void renewCache(ProceedingJoinPoint pjp) throws Throwable {
        RedisCacheService.renewCache(pjp);
    }
}