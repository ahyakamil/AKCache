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
import redis.clients.jedis.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
    private static int maxTotalPoolStatic = 8;
    private static int maxIdlePoolStatic = 8;
    private static int minIdlePoolStatic = 0;
    private static boolean isUsingPoolStatic = false;
    private static JedisPool JEDIS_POOL;
    private static JedisPool JEDIS_POOL_ASYNC;


    public static void setupConnection(String host, int port, String username, String password, int maxTotalPool, int maxIdlePool, int minIdlePool, boolean isUsingPool) {
        hostStatic = host;
        portStatic = port;
        usernameStatic = username;
        passwordStatic = password;
        maxTotalPoolStatic = maxTotalPool;
        maxIdlePoolStatic = maxIdlePool;
        minIdlePoolStatic = minIdlePool;
        isUsingPoolStatic = isUsingPool;
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotalPoolStatic);
        poolConfig.setMaxIdle(maxIdlePoolStatic);
        poolConfig.setMinIdle(minIdlePoolStatic);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    private static void checkJedis(String host, int port, String username, String password, boolean isAsync) {
        try {
            if(isAsync) {
                JEDIS_ASYNC.get("forCheckConnection");
                logger.debug("still connected...");
            } else {
                JEDIS.get("forCheckConnection");
                logger.debug("still connected...");
            }
        } catch (Exception e) {
            logger.debug("not connected...");
            openConnetion(host, port, username, password, isAsync);
        }
    }

    private static void openConnetion(String host, int port, String username, String password, boolean isAsync) {
        Jedis jedis;
        if(isUsingPoolStatic) {
            if(isAsync) {
                if(JEDIS_POOL_ASYNC != null) {
                    JEDIS_POOL_ASYNC.close();
                }
            } else {
                if(JEDIS_POOL != null) {
                    JEDIS_POOL.close();
                }
            }
            JedisPoolConfig jedisPoolConfig = buildPoolConfig();
            JedisPool jedisPool;
            if(StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                jedisPool = new JedisPool(jedisPoolConfig, host, port, 2000, password);
            } else if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                jedisPool = new JedisPool(jedisPoolConfig, host, port, 2000, username, password);
            } else {
                jedisPool = new JedisPool(jedisPoolConfig, host, port, 2000);
            }
            if(isAsync) {
                JEDIS_POOL_ASYNC = jedisPool;
                JEDIS_ASYNC = jedisPool.getResource();
            } else {
                JEDIS_POOL = jedisPool;
                JEDIS = jedisPool.getResource();
            }
        } else {
            jedis = new Jedis(host, port);
            if(StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                jedis.auth(password);
            } else if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                jedis.auth(username, password);
            }
            if(isAsync) {
                JEDIS_ASYNC = jedis;
            } else {
                JEDIS = jedis;
            }
        }
        logger.debug("successfully connect to redis...");
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
            checkJedis(hostStatic, portStatic, usernameStatic, passwordStatic, false);
            ObjectMapper objectMapper = new ObjectMapper();
            String key = getKey(pjp);
            int ttl = getTtl(pjp);
            String conditionRegex = method.getAnnotation(AKCache.class).conditionRegex();

            List<String> keys = findKeys(key, JEDIS);
            if (keys.size() > 0) {
                byte[] foundedKey = keys.iterator().next().getBytes();
                byte[] bytes = JEDIS.hget(foundedKey, "objValue".getBytes());
                logger.debug("key exist, get from cache");
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                Object deSerialize = objectMapper.readValue(bytes, returnedClass);
                return deSerialize;
            } else {
                return createCache(JEDIS_ASYNC, pjp, key, ttl, conditionRegex, null);
            }
        } catch (Exception e) {
            logger.debug("get data failed...");
            logger.debug(e.getMessage());
            return pjp.proceed();
        } finally {
            logger.debug("close redis..");
            JEDIS.close();
        }
    }

    private static List<String> findKeys(String key, Jedis jedis) {
        String findKey = key;
        String keyPattern = escapeMetaCharacters(findKey) + ":*";
        logger.debug("key to find: " + keyPattern);
        ScanParams scanParams = new ScanParams().match(keyPattern).count(10);
        String cur = ScanParams.SCAN_POINTER_START;
        List<String> keys = new ArrayList<>();
        do {
            ScanResult scanResult = jedis.scan(cur, scanParams);
            keys.addAll(scanResult.getResult());
            cur = scanResult.getCursor();
            if(keys.size() > 0) {
                break;
            }
        } while (!cur.equals(ScanParams.SCAN_POINTER_START));
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
        checkJedis(hostStatic, portStatic, usernameStatic, passwordStatic, true);
        String key = getKey(pjp);
        List<String> keys = findKeys(key, JEDIS_ASYNC);
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

    private static void doRenewCache(List<String> oldKeys, ProceedingJoinPoint pjp, Jedis jedis) throws Throwable {
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

    private static Object createCache(Jedis jedis, ProceedingJoinPoint pjp, String key, int ttl, String conditionRegex, List<String> oldKeys) throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        Object proceed = pjp.proceed();
        key += ":return_" + objectMapper.writeValueAsString(proceed);
        logger.debug("key : " + key);
        Pattern pattern = Pattern.compile(conditionRegex);
        Matcher matcher = pattern.matcher(key);
        if(matcher.find()) {
            doCreate(oldKeys, key, jedis, proceed, ttl);
        }
        return proceed;
    }

    private static void doCreate(List<String> oldKeys, String key, Jedis jedis, Object proceed, int ttl) {
        new Thread(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                logger.debug("serializing obj...");
                logger.debug("key bytes : " + key.getBytes());
                ForceObjToSerialize forceObjToSerialize = new ForceObjToSerialize(proceed);
                jedis.hset(key.getBytes(), "objValue".getBytes(), objectMapper.writeValueAsBytes(forceObjToSerialize.getValueObj()));
                jedis.expire(key.getBytes(), ttl);
                logger.debug("successfully create cache");

                //remove unused keys
                if(oldKeys != null && oldKeys.size() > 0) {
                    for(String oldKey: oldKeys) {
                        if(!oldKey.equals(key)) {
                            jedis.del(oldKey);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                jedis.close();
            }
        }).start();
    }
}
