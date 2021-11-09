package com.ahyakamil.AKCache.service;

import com.ahyakamil.AKCache.AKCacheSetup;
import com.ahyakamil.AKCache.annotation.AKCache;
import com.ahyakamil.AKCache.annotation.AKCacheUpdate;
import com.ahyakamil.AKCache.constant.AKConstants;
import com.ahyakamil.AKCache.constant.UpdateType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.RedisCommand;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static String hostStatic;
    private static int portStatic;
    private static String usernameStatic;
    private static String passwordStatic;
    private static int maxTotalPoolStatic = 8;
    private static int maxIdlePoolStatic = 8;
    private static int minIdlePoolStatic = 0;
    private static boolean isUsingPoolStatic = false;
    private static RedisCommands<String, String> REDIS_SYNC;

    public static void setupConnection(String host, int port, String username, String password, int maxTotalPool, int maxIdlePool, int minIdlePool, boolean isUsingPool) {
        hostStatic = host;
        portStatic = port;
        usernameStatic = username;
        passwordStatic = password;
        maxTotalPoolStatic = maxTotalPool;
        maxIdlePoolStatic = maxIdlePool;
        minIdlePoolStatic = minIdlePool;
        isUsingPoolStatic = isUsingPool;
        openConnetion(host, port, username, password);
    }

    private static void openConnetion(String host, int port, String username, String password) {
        RedisURI.Builder redisURIBuilder = RedisURI.builder();
        redisURIBuilder.withHost(host);
        redisURIBuilder.withPort(port);
        if(!password.isEmpty()) {
            redisURIBuilder.withPassword(password);
        }
        if(!username.isEmpty()) {
            redisURIBuilder.withClientName(username);
        }

        RedisClient redisClient = RedisClient.create(redisURIBuilder.build());
        StatefulRedisConnection<String, String> statefulRedisConnection = redisClient.connect();
        REDIS_SYNC = statefulRedisConnection.sync();
        logger.debug("successfully connect to redis...");
    }

    public static Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        if(getMethod(pjp).getAnnotation(AKCacheUpdate.class) != null) {
            return null;
        } else if(getMethod(pjp).getAnnotation(AKCache.class) != null) {
            Class returnedClass = getMethod(pjp).getAnnotation(AKCache.class).serializeClass();
            return getData(pjp, getMethod(pjp), returnedClass);
        }
        return null;
    }

    private static Method getMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return method;
    }

    private static String getId(ProceedingJoinPoint pjp) {
        return getMethod(pjp).getAnnotation(AKCache.class).id();
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
        ObjectMapper objectMapper = new ObjectMapper();
        int ttl = getTtl(pjp);
        String conditionRegex = method.getAnnotation(AKCache.class).conditionRegex();

        List<String> keys = findKeys(pjp);
        if (keys.size() > 0) {
            String foundedKey = keys.get(0);
            String objValue = REDIS_SYNC.hget(foundedKey, "objValue");
            logger.debug("key exist, get from cache");
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Object deSerialize = objectMapper.readValue(objValue, returnedClass);
            return deSerialize;
        } else {
            Object proceed = pjp.proceed();
            return proceed;
        }
    }

    private static List<String> findKeys(ProceedingJoinPoint pjp) throws JsonProcessingException {
        String id = getId(pjp);
        String findKey = "";
        String keyPattern = "";
        if(id.trim().isEmpty()) {
            findKey = getKey(pjp);
            keyPattern = escapeMetaCharacters(findKey) + ":return*";
        } else {
            findKey = getKeyWithId(pjp);
            keyPattern = findKey + ":return*";
        }
        logger.debug("key to find: " + keyPattern);
        ScanArgs scanArgs = new ScanArgs();
        scanArgs.limit(10000);
        scanArgs.match(keyPattern);
        List<String> keys =  new ArrayList<>();
        KeyScanCursor keyScanCursor = REDIS_SYNC.scan(scanArgs);
        List<String> foundedKeys = keyScanCursor.getKeys();
        keys.addAll(foundedKeys);
        logger.debug("===> founded keys: " + keys.size());
        if(keys.size() > 0) {
            keyScanCursor.setFinished(true);
        }
        while (!keyScanCursor.isFinished()) {
            foundedKeys = keyScanCursor.getKeys();
            keys.addAll(foundedKeys);
            keyScanCursor = REDIS_SYNC.scan(keyScanCursor, scanArgs);
            logger.debug("===> cursor: " + keyScanCursor.getCursor());
            if(keys.size() > 0) {
                keyScanCursor.setFinished(true);
            }
        }
        return keys;
    }

    private static String getKey(ProceedingJoinPoint pjp) throws JsonProcessingException {
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        Gson gson = new Gson();
        paramsKey += gson.toJson(args);
        UpdateType updateType = getUpdateType(pjp);
        String key = pjp.getTarget().getClass().getName() + ":" + getMethod(pjp).getName() + ":updateType_" + updateType + ":args_" + paramsKey;
        return key;
    }

    private static String getKeyWithId(ProceedingJoinPoint pjp) {
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        Gson gson = new Gson();
        String json = gson.toJson(args);
        logger.debug("===> args: " + json);
        UpdateType updateType = getUpdateType(pjp);
        String id = getId(pjp);
        String valueId = getValueFromJson(json, id);
        logger.debug("===> valueId: " + valueId);
        String key = pjp.getTarget().getClass().getName() + ":" + getMethod(pjp).getName() + ":updateType_" + updateType + ":args_*" + valueId + "*";
        return key;
    }

    private static String getValueFromJson(String json, String key) {
        Gson gson = new Gson();
        String value = "";
        char[] chars = key.toCharArray();
        String strToFind = "";
        String jsonToFind = json;
        boolean isOpenToWrite = false;
        for(int i=0; i < key.length(); i++) {
            if(chars[i] == '[') {
                isOpenToWrite = true;
            } else if(chars[i] == ']') {
                isOpenToWrite = false;
            } else if(chars[i] == '.') {
                isOpenToWrite = true;
            }


            if(isOpenToWrite == true) {
                strToFind += chars[i];
                int nextChar = i+1;
                if((nextChar) < key.length()) {
                    if(chars[nextChar] == '.') {
                        isOpenToWrite = false;
                    }
                }
            }

            if(i == (key.length()-1)) {
                isOpenToWrite = false;
            }

            if((isOpenToWrite == false && !strToFind.isEmpty())) {
                logger.debug("===> strToFind " + strToFind);
                logger.debug("===> jsonToFind: " + jsonToFind);
                JsonElement jsonElement = new JsonParser().parse(jsonToFind);
                if(strToFind.contains("[")) {
                    strToFind = strToFind.replace("[", "");
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    JsonElement checkJson = jsonArray.get(Integer.parseInt(strToFind));
                    if(checkJson.isJsonArray()) {
                        jsonToFind = gson.toJson(checkJson.getAsJsonArray());
                    } else if(checkJson.isJsonObject()) {
                        jsonToFind = gson.toJson(checkJson.getAsJsonObject());
                    } else {
                        jsonToFind = checkJson.getAsString();
                    }
                    value = jsonToFind;
                } else {
                    strToFind = strToFind.replace(".", "");
                    try {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        jsonToFind = gson.toJson(jsonObject.get(strToFind));
                    } catch (UnsupportedOperationException e) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        jsonToFind = gson.toJson(jsonObject.get(strToFind).getAsString());
                    }
                    value = jsonToFind;
                }
                strToFind = "";
            }
        }
        value = value.replaceAll("\"", "");
        return value;
    }

    public static void renewCache(ProceedingJoinPoint pjp) throws Throwable {
        List<String> keys = findKeys(pjp);

        logger.debug("==> renewCache() key size: " + keys.size());
        String onloadingKey = "onloading_" + getKey(pjp);
        String isOnloading = REDIS_SYNC.hget(onloadingKey, "value");
        if(isOnloading == null) {
            REDIS_SYNC.hset(onloadingKey, "value", "ok");
            REDIS_SYNC.expire(onloadingKey, 10);
            if(getUpdateType(pjp).equals(UpdateType.FETCH)) {
                doRenewCache(keys, pjp);
            } else if(getUpdateType(pjp).equals(UpdateType.SMART)) {
                if(isTimeToRenewCache(getTtl(pjp), REDIS_SYNC.ttl(keys.iterator().next()))) {
                    doRenewCache(keys, pjp);
                }
            }
        } else {
            logger.debug("process still loading...");
        }
    }

    private static void doRenewCache(List<String> oldKeys, ProceedingJoinPoint pjp) throws Throwable {
        logger.debug("it's time to renew cache...");
        createCache(pjp.proceed(), pjp, getTtl(pjp), getConditionRegex(pjp), oldKeys);
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

    private static void createCache(Object proceed, ProceedingJoinPoint pjp, int ttl, String conditionRegex, List<String> oldKeys) throws Throwable {
        String key = getKey(pjp);
        ObjectMapper objectMapper = new ObjectMapper();
        key += ":return_" + objectMapper.writeValueAsString(proceed);
        logger.debug("key : " + key);
        Pattern pattern = Pattern.compile(conditionRegex);
        Matcher matcher = pattern.matcher(key);
        if(matcher.find()) {
            doCreate(oldKeys, key, proceed, ttl, pjp);
        }
    }

    private static void doCreate(List<String> oldKeys, String key, Object proceed, int ttl, ProceedingJoinPoint pjp) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        logger.debug("serializing obj...");
        logger.debug("key bytes : " + key.getBytes());
        ForceObjToSerialize forceObjToSerialize = new ForceObjToSerialize(proceed);
        REDIS_SYNC.hset(key, "objValue", objectMapper.writeValueAsString(forceObjToSerialize.getValueObj()));
        REDIS_SYNC.expire(key, ttl);
        logger.debug("successfully create cache");

        //remove unused keys
        if (oldKeys != null && oldKeys.size() > 0) {
            for (String oldKey : oldKeys) {
                if (!oldKey.equals(key)) {
                    REDIS_SYNC.del(oldKey);
                }
            }
        }
        String onloadingKey = "onloading_" + getKey(pjp);
        REDIS_SYNC.del(onloadingKey);
    }
}
