package com.ahyakamil.AKCache.service;

import com.ahyakamil.AKCache.AKCacheSetup;
import com.ahyakamil.AKCache.annotation.AKCache;
import com.ahyakamil.AKCache.annotation.AKCacheUpdate;
import com.ahyakamil.AKCache.constant.AKConstants;
import com.ahyakamil.AKCache.constant.UpdateType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ahyakamil.AKCache.util.AKUtils.escapeMetaCharacters;

public class RedisCacheService {
    private static Logger logger = LoggerFactory.getLogger(AKCacheSetup.class);
    private static Jedis JEDIS;
    private static ProceedingJoinPoint pjpStatic;
    private static Set<String> foundedKeysStatic = new HashSet<>();
    private static int ttlStatic;
    private static String keyStatic;
    private static UpdateType updateTypeStatic;
    private static String conditionRegexStatic;
    private static String currentKeyStatic;

    public static void setupConnection(String host, int port, String username, String password) {
        try {
            JEDIS = new Jedis(host, port);
            if(StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                JEDIS.auth(password);
            } else if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                JEDIS.auth(username, password);
            }
            JEDIS.connect();
            logger.info("Successfully connect to redis...");
        } catch (Exception e) {
            JEDIS = null;
            new Thread(() -> {
                logger.info("failed connect to redis, retry in 10s...");
                try {
                    Thread.sleep(10000);
                    setupConnection(host, port, username, password);
                } catch (Exception eSleep) {
                    logger.debug(eSleep.getMessage());
                }
            }).start();
        }
    }

    public static Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class returnedClass;
        try {
            returnedClass = method.getAnnotation(AKCacheUpdate.class).serializeClass();
            return null;
        } catch (NullPointerException e) {
            returnedClass = method.getAnnotation(AKCache.class).serializeClass();
            return getData(pjp, method, returnedClass);
        }
    }

    private static Object getData(ProceedingJoinPoint pjp, Method method, Class returnedClass) throws Throwable {
        if(JEDIS == null) {
            return pjp.proceed();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        paramsKey += objectMapper.writeValueAsString(args);
        UpdateType updateType = method.getAnnotation(AKCache.class).updateType();
        String key = pjp.getTarget().getClass().getName() + ":" + method.getName() + ":updateType_" + updateType + ":args_" + paramsKey;
        int ttl = method.getAnnotation(AKCache.class).ttl();
        String conditionRegex = method.getAnnotation(AKCache.class).conditionRegex();

        String findKey = key;
        String keyPattern = escapeMetaCharacters(findKey) + ":*";
        logger.debug("key to find: " + keyPattern);
        Set<String> keys = JEDIS.keys(keyPattern);
        if (keys.size() > 0) {
            byte[] foundedKey = keys.iterator().next().getBytes();
            byte[] bytes = JEDIS.hget(foundedKey, "objValue".getBytes());
            logger.debug("key exist, get from cache");
            pjpStatic = pjp;
            ttlStatic = ttl;
            keyStatic = key;
            foundedKeysStatic = keys;
            updateTypeStatic = updateType;
            conditionRegexStatic = conditionRegex;
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Object deSerialize = objectMapper.readValue(bytes, returnedClass);
            return deSerialize;
        } else {
            return createCache(pjp, key, ttl, conditionRegex);
        }
    }

    public static void renewCache() throws Throwable {
        if(JEDIS != null) {
            if(foundedKeysStatic.size() > 0) {
                if(updateTypeStatic.equals(UpdateType.FETCH)) {
                    doRenewCache();
                } else if(updateTypeStatic.equals(UpdateType.SMART)) {
                    if(isTimeToRenewCache(ttlStatic, JEDIS.ttl(foundedKeysStatic.iterator().next().getBytes()))) {
                        doRenewCache();
                    }
                }
            }
        }
    }

    private static void doRenewCache() throws Throwable {
        logger.debug("it's time to renew cache...");
        createCache(pjpStatic, keyStatic, ttlStatic, conditionRegexStatic);

        // delete unused keys
        if(foundedKeysStatic.size() > 1) {
            for(String oldKey: foundedKeysStatic) {
                if(!oldKey.equals(currentKeyStatic)) {
                    JEDIS.del(oldKey);
                }
            }
        }
    }

    private static boolean isTimeToRenewCache(int ttl, Long currentTtl) {
        float timeToRenew = (float) ttl - ((float) ttl * ((float) AKConstants.PERCENTAGE_TO_RENEW_CACHE/100));
        logger.debug("ttl : " + ttl);
        logger.debug("time to renew : " + timeToRenew);
        logger.debug("current ttl : " + currentTtl);
        if(currentTtl <= timeToRenew) {
            return true;
        }
        return false;
    }

    private static Object createCache(ProceedingJoinPoint pjp, String key, int ttl, String conditionRegex) throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        Object proceed = pjp.proceed();
        key += ":return_" + objectMapper.writeValueAsString(proceed);
        logger.debug("key : " + key);
        Pattern pattern = Pattern.compile(conditionRegex);
        Matcher matcher = pattern.matcher(key);
        if(matcher.find()) {
            logger.debug("serializing obj...");
            logger.debug("key bytes : " + key.getBytes());
            ForceObjToSerialize forceObjToSerialize = new ForceObjToSerialize(proceed);
            JEDIS.hset(key.getBytes(), "objValue".getBytes(), objectMapper.writeValueAsBytes(forceObjToSerialize.getValueObj()));
            JEDIS.expire(key.getBytes(), ttl);
            logger.debug("successfully create cache");
        }
        currentKeyStatic = key;
        return proceed;
    }
}
