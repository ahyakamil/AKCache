package com.ahyakamil.AKCache.sample;

import com.ahyakamil.AKCache.annotation.AKCache;
import com.ahyakamil.AKCache.annotation.AKCacheUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sample {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @AKCache
    public String case1() {
        logger.info("I'm in method..");
        return "What a beautiful day!";
    }

    @AKCache(ttl = 180, id = "[0]")
    public String case2(String quote) {
        logger.info("I'm in method..");
        return quote;
    }

    @AKCacheUpdate(targetRegex = "")
    public String case3(String quote) {
        logger.info("I'm in method..");
        return quote;
    }
}
