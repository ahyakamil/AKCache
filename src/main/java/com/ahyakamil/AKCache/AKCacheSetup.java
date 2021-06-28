package com.ahyakamil.AKCache;

import com.ahyakamil.AKCache.constant.UpdateType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ahyakamil.AKCache.annotation.AKCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

public class AKCacheSetup {
    private static Logger logger = LoggerFactory.getLogger(AKCacheSetup.class);
    private static Jedis JEDIS;

    public static void setupConnection(String host, int port, String username, String password) {
        JEDIS = new Jedis(host, port);
        if(StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            JEDIS.auth(password);
        } else if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            JEDIS.auth(username, password);
        }
        logger.info("Successfully connect to redis...");
    }

    private static Object setListener(ProceedingJoinPoint pjp, Class returnedClass) throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        paramsKey += objectMapper.writeValueAsString(args);
        UpdateType updateType = method.getAnnotation(AKCache.class).updateType();
        String key = pjp.getTarget().getClass().getName() + ":" + method.getName() + ":updateType_" + updateType + ":args_" + paramsKey;
        int ttl = method.getAnnotation(AKCache.class).ttl();

        String findKey = key;
        String keyPattern = escapeMetaCharacters(findKey) + ":*";
        logger.debug("key to find: " + keyPattern);
        Set<String> keys = JEDIS.keys(keyPattern);
        if (keys.size() > 0) {
            byte[] bytes = JEDIS.hget(keys.iterator().next().getBytes(), "objValue".getBytes());
            logger.debug("key exist, get from cache");
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Object deSerialize = objectMapper.readValue(bytes, returnedClass);
            return deSerialize;
        } else {
            Object procced = pjp.proceed();
            key += ":return_" + objectMapper.writeValueAsString(procced);

            logger.debug("serializing obj...");
            logger.debug("key bytes : " + key.getBytes());
            ForceObjToSerialize forceObjToSerialize = new ForceObjToSerialize(procced);
            JEDIS.hset(key.getBytes(), "objValue".getBytes(), objectMapper.writeValueAsBytes(forceObjToSerialize.getValueObj()));
            JEDIS.expire(key.getBytes(), ttl);
            return procced;
        }
    }

    public static Object setListener(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class returnedClass = method.getAnnotation(AKCache.class).serializeClass();
        return setListener(pjp, returnedClass);
    }

    public static String escapeMetaCharacters(String inputString){
        final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")","*","+","?","|","<",">","-","&","%"};

        for (int i = 0 ; i < metaCharacters.length ; i++){
            if(inputString.contains(metaCharacters[i])){
                inputString = inputString.replace(metaCharacters[i],"\\"+metaCharacters[i]);
            }
        }
        return inputString;
    }
}

class ForceObjToSerialize<T> extends ObjNotSerialize<T> implements Serializable {
    private static final long serialVersionUID = 11111L;

    public ForceObjToSerialize(T valueObj) {
        super(valueObj);
    }

    public ForceObjToSerialize() {
    }

    @Override
    public T getValueObj() {
        return super.getValueObj();
    }
}

class ObjNotSerialize<T> {
    public T getValueObj() {
        return valueObj;
    }

    T valueObj;

    public ObjNotSerialize(T valueObj) {
        this.valueObj = valueObj;
    }

    public ObjNotSerialize() {

    }
}