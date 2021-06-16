package com.kipaskipas.cache.config;

import com.kipaskipas.cache.KipaskipasCacheSetup;
import com.kipaskipas.cache.annotation.KipaskipasCache;
import com.kipaskipas.cache.utils.SerializeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
public class AspectConfig {
    private static Logger logger = LoggerFactory.getLogger(AspectConfig.class);

    @Around("execution(* *(..)) && @annotation(com.kipaskipas.cache.annotation.KipaskipasCache)")
    public Object listener(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        for(int argIndex=0; argIndex < args.length; argIndex++) {
            Parameter parameter = method.getParameters()[argIndex];
            paramsKey += parameter.getType().getName() + "_";
            paramsKey += parameter.getName() + "(" + args[argIndex] + ")" + "_";
        }
        String key = pjp.getTarget().getClass().getName() + "::" + method.getName() + "::" + paramsKey;
        logger.debug("before method: " + key);
        String keyAnnotation = method.getAnnotation(KipaskipasCache.class).key();
        logger.debug("key annotation: " + keyAnnotation);
        Object procced = pjp.proceed();
        logger.debug("return value: " + procced);

        KipaskipasCacheSetup.JEDIS.hset(key.getBytes(), "test".getBytes(), SerializeUtils.serialize(procced));

        byte[] deserialize = KipaskipasCacheSetup.JEDIS.hget(key.getBytes(), "test".getBytes());
        logger.debug("successfully deserialize obj: " + SerializeUtils.deSerialize(deserialize));
        return procced;
    }
}
