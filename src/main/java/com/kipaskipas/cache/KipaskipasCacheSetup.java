package com.kipaskipas.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kipaskipas.cache.annotation.KipaskipasCache;
import com.kipaskipas.cache.constant.UpdateType;
import com.kipaskipas.cache.utils.SerializeUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

public class KipaskipasCacheSetup<T> {
    private static Logger logger = LoggerFactory.getLogger(KipaskipasCacheSetup.class);
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

    public T setListener(ProceedingJoinPoint pjp, Class<T> returnedClass) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String paramsKey = "";
        Object[] args = pjp.getArgs();
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            Parameter parameter = method.getParameters()[argIndex];
            paramsKey += parameter.getType().getName() + "_";
            paramsKey += parameter.getName() + "(" + args[argIndex] + ")" + "__";
        }
        UpdateType updateType = method.getAnnotation(KipaskipasCache.class).updateType();
        String key = pjp.getTarget().getClass().getName() + ":" + method.getName() + ":updateType_" + updateType + ":args_" + paramsKey;

        String findKey = key;
        String keyPattern = escapeMetaCharacters(findKey) + ":*";
        logger.debug("key to find: " + keyPattern);
        Set<String> keys = JEDIS.keys(keyPattern);
        if (keys.size() > 0) {
            byte[] bytes = JEDIS.hget(keys.iterator().next().getBytes(), "objValue".getBytes());
            logger.debug("key exist, get from cache");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            T deSerialize = objectMapper.readValue(bytes, returnedClass);
            return deSerialize;
        } else {
            T procced = (T) pjp.proceed();
            ObjectMapper objectMapper = new ObjectMapper();
            key += ":return_" + objectMapper.writeValueAsString(procced);

            logger.debug("serializing obj...");
            logger.debug("key bytes : " + key.getBytes());
            ForceObjToSerialize<T> forceObjToSerialize = new ForceObjToSerialize(procced);
            JEDIS.hset(key.getBytes(), "objValue".getBytes(), objectMapper.writeValueAsBytes(forceObjToSerialize.getValueObj()));
            return procced;
        }
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