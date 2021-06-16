package com.kipaskipas.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class KipaskipasCacheSetup {
    private static Logger logger = LoggerFactory.getLogger(KipaskipasCacheSetup.class);
    public static Jedis JEDIS;

    public static void setup(String host, int port, String username, String password) {
        JEDIS = new Jedis(host, port);
        if(StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            JEDIS.auth(password);
        } else if(!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            JEDIS.auth(username, password);
        }
        logger.info("Successfully connect to redis");
    }
}
