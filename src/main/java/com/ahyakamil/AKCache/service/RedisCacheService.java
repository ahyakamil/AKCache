package com.ahyakamil.AKCache.service;

import com.ahyakamil.AKCache.AKCacheSetup;
import com.ahyakamil.AKCache.annotation.AKCache;
import com.ahyakamil.AKCache.annotation.AKCacheUpdate;
import com.ahyakamil.AKCache.constant.AKConstants;
import com.ahyakamil.AKCache.constant.UpdateType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ahyakamil.AKCache.util.AKUtils.escapeMetaCharacters;

public class RedisCacheService {
    private static Logger logger = LoggerFactory.getLogger(AKCacheSetup.class);
    private static Jedis JEDIS;
    private static Jedis JEDIS_ASYNC;
    private static String hostStatic;
    private static int portStatic;
    private static String usernameStatic;
    private static String passwordStatic;


    public static void setupConnection(String host, int port, String username, String password) {
        hostStatic = host;
        portStatic = port;
        usernameStatic = username;
        passwordStatic = password;
        JEDIS = openConnetion(host, port, username, password);
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(50);
        poolConfig.setMinIdle(10);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    private static Jedis getJedis(Jedis jedis, String host, int port, String username, String password) {
        try {
            jedis.get("forCheckConnection");
            logger.debug("still connected...");
            return jedis;
        } catch (Exception e) {
            logger.debug("not connected...");
            jedis = openConnetion(host, port, username, password);
            return jedis;
        }
    }

    private static Jedis openConnetion(String host, int port, String username, String password) {
        JedisPoolConfig jedisPoolConfig = buildPoolConfig();
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, host, port);
        Jedis jedis = jedisPool.getResource();
        if(StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            jedis.auth(password);
        } else if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            jedis.auth(username, password);
        }
        logger.debug("successfully connect to redis...");
        return jedis;
    }

    public static Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        Class returnedClass;
        try {
            returnedClass = getMethod(pjp).getAnnotation(AKCacheUpdate.class).serializeClass();
            return null;
        } catch (NullPointerException e) {
            returnedClass = getMethod(pjp).getAnnotation(AKCache.class).serializeClass();
            return getData(pjp, getMethod(pjp), returnedClass);
        }
    }

    private static Method getMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return method;
    }

    private static UpdateType getUpdateType(ProceedingJoinPoint pjp) {
        return getMethod(pjp).getAnnotation(AKCache.class).updateType();
    }

    private static int getTtl(ProceedingJoinPoint pjp) {
        return getMethod(pjp).getAnnotation(AKCache.class).ttl();
    }

    private static String getConditionRegex(ProceedingJoinPoint pjp) {
        return getMethod(pjp).getAnnotation(AKCache.class).conditionRegex();
    }

    private static Object getData(ProceedingJoinPoint pjp, Method method, Class returnedClass) throws Throwable {
        try {
            JEDIS = getJedis(JEDIS, hostStatic, portStatic, usernameStatic, passwordStatic);
            ObjectMapper objectMapper = new ObjectMapper();
            String key = getKey(pjp);
            int ttl = getTtl(pjp);
            String conditionRegex = method.getAnnotation(AKCache.class).conditionRegex();

            Set<String> keys = findKeys(key, JEDIS);
            if (keys.size() > 0) {
                byte[] foundedKey = keys.iterator().next().getBytes();
                byte[] bytes = JEDIS.hget(foundedKey, "objValue".getBytes());
                logger.debug("key exist, get from cache");
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                Object deSerialize = objectMapper.readValue(bytes, returnedClass);
                return deSerialize;
            } else {
                return createCache(JEDIS, pjp, key, ttl, conditionRegex, null);
            }
        } catch (Exception e) {
            logger.debug("get data failed...");
            logger.debug(e.getMessage());
            return pjp.proceed();
        }
    }

    private static Set<String> findKeys(String key, Jedis jedis) {
        String findKey = key;
        String keyPattern = escapeMetaCharacters(findKey) + ":*";
        logger.debug("key to find: " + keyPattern);
        Set<String> keys = jedis.keys(keyPattern);
        return keys;
    }

    private static String getKey(ProceedingJoinPoint pjp) throws JsonProcessingException {
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        Gson gson = new Gson();
        paramsKey += gson.toJson(args);
        UpdateType updateType = getMethod(pjp).getAnnotation(AKCache.class).updateType();
        String key = pjp.getTarget().getClass().getName() + ":" + getMethod(pjp).getName() + ":updateType_" + updateType + ":args_" + paramsKey;
        return key;
    }

    public static void renewCache(ProceedingJoinPoint pjp) throws Throwable {
        JEDIS_ASYNC = getJedis(JEDIS_ASYNC, hostStatic, portStatic, usernameStatic, passwordStatic);
        String key = getKey(pjp);
        Set<String> keys = findKeys(key, JEDIS_ASYNC);
        logger.debug("==> renewCache() key size: " + keys.size());
        if(keys.size() > 0) {
            if(getUpdateType(pjp).equals(UpdateType.FETCH)) {
                doRenewCache(keys, pjp, JEDIS_ASYNC);
            } else if(getUpdateType(pjp).equals(UpdateType.SMART)) {
                if(isTimeToRenewCache(getTtl(pjp), JEDIS_ASYNC.ttl(keys.iterator().next().getBytes()))) {
                    doRenewCache(keys, pjp, JEDIS_ASYNC);
                }
            }
        }
    }

    private static void doRenewCache(Set<String> oldKeys, ProceedingJoinPoint pjp, Jedis jedis) throws Throwable {
        logger.debug("it's time to renew cache...");
        createCache(jedis, pjp, getKey(pjp), getTtl(pjp), getConditionRegex(pjp), oldKeys);
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

    private static Object createCache(Jedis jedis, ProceedingJoinPoint pjp, String key, int ttl, String conditionRegex, Set<String> oldKeys) throws Throwable {
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
            jedis.hset(key.getBytes(), "objValue".getBytes(), objectMapper.writeValueAsBytes(forceObjToSerialize.getValueObj()));
            jedis.expire(key.getBytes(), ttl);
            logger.debug("successfully create cache");
        }

        //remove unused keys
        if(oldKeys != null && oldKeys.size() > 0) {
            for(String oldKey: oldKeys) {
                if(!oldKey.equals(key)) {
                    jedis.del(oldKey);
                }
            }
        }
        return proceed;
    }
}
