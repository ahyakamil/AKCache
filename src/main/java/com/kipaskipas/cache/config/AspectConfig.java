package com.kipaskipas.cache.config;

import com.kipaskipas.cache.KipaskipasCacheSetup;
import com.kipaskipas.cache.annotation.KipaskipasCache;
import com.kipaskipas.cache.constant.UpdateType;
import com.kipaskipas.cache.utils.SerializeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

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
            paramsKey += parameter.getName() + "(" + args[argIndex] + ")" + "__";
        }
        UpdateType updateType = method.getAnnotation(KipaskipasCache.class).updateType();
        String key = pjp.getTarget().getClass().getName() + ":" + method.getName() + ":updateType_" + updateType + ":args_" + paramsKey;

        String findKey = key + "*";
        Set<String> keys = KipaskipasCacheSetup.JEDIS.keys(findKey);
        byte[] bytes = KipaskipasCacheSetup.JEDIS.hget(keys.iterator().next().getBytes(), "objValue".getBytes());
        if(bytes != null) {
            logger.debug("key exist, get from cache");
            return SerializeUtils.deSerialize(bytes);
        } else {
            Object procced = pjp.proceed();
            key += ":return_";
            if(procced.getClass().isAssignableFrom(String.class)) {
                key += "str_" + procced + "__";
            } else {
                for(Field field: procced.getClass().getDeclaredFields()) {
                    String fieldName = field.getName();
                    field.setAccessible(true);
                    Object fieldValue = field.get(procced);
                    key += fieldName + "_" + fieldValue + "__";
                }
            }

            KipaskipasCacheSetup.JEDIS.hset(key.getBytes(), "objValue".getBytes(), SerializeUtils.serialize(procced));
            return procced;
        }
    }
}
