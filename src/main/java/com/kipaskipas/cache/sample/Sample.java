package com.kipaskipas.cache.sample;

import com.kipaskipas.cache.annotation.KipaskipasCache;
import com.kipaskipas.cache.constant.UpdateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Sample {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @KipaskipasCache(updateType = UpdateType.SMART)
    public String sampleCache(String paramText) {
        logger.debug("hello I'm executed");
        return "hello from sample cache " + paramText + " " + LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    @KipaskipasCache(updateType = UpdateType.FETCH)
    public String sampleCache() {
        logger.debug("hello I'm executed");
        return "hello from okesip";
    }
}
