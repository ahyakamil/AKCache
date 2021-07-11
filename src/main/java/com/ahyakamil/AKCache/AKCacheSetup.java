package com.ahyakamil.AKCache;

import com.ahyakamil.AKCache.service.RedisCacheService;
import com.ahyakamil.AKCache.annotation.AKCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.reflect.Method;

public class AKCacheSetup {
    public static void setupConnection(String host, int port, String username, String password) {
        RedisCacheService.setupConnection(host, port, username, password);
    }

    public static Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        return RedisCacheService.setListener(pjp);
    }
}