package com.kipaskipas.cache.config;

import com.kipaskipas.cache.annotation.KipaskipasCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@Aspect
public class AspectConfig {
    private static Logger logger = LoggerFactory.getLogger(AspectConfig.class);

    @Around("execution(* *(..)) && @annotation(com.kipaskipas.cache.annotation.KipaskipasCache)")
    public Object listener(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String paramsKey = "";
        for(Class cl: method.getParameterTypes()) {
            paramsKey += cl.getName() + "_";
        }
        String key = pjp.getTarget().getClass() + ":" + method.getName() + ":" + paramsKey;
        logger.info("before method " + key + " run");
        String keyAnnotation = method.getAnnotation(KipaskipasCache.class).key();
        logger.info("key annotation: " + keyAnnotation);
        Object procced = pjp.proceed();
        logger.info("return value: " + procced);
        return procced;
    }
}
