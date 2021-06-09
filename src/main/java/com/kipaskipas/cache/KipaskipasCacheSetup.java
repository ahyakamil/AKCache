package com.kipaskipas.cache;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KipaskipasCacheSetup {
    private static Logger logger = LoggerFactory.getLogger(KipaskipasCacheSetup.class);

    public static void setup(String url) {
        RedisClient redisClient = new RedisClient(
                RedisURI.create("redis://" + url)
        );
        RedisConnection<String, String> connection = redisClient.connect();
        logger.info("Successfully connect to redis");
    }
}
