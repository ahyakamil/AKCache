package com.ahyakamil.AKCache.config;

import com.ahyakamil.AKCache.AKCacheSetup;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AspectConfig {
    @Around("execution(* *(..)) && @annotation(com.ahyakamil.AKCache.annotation.AKCache) || @annotation(com.ahyakamil.AKCache.annotation.AKCacheUpdate)")
    public Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        return AKCacheSetup.getCache(pjp);
    }
}
