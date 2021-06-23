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
    public SampleDto sampleCache(String paramText) {
        SampleDto result = new SampleDto();
        logger.debug("hello I'm executed");
        result.setObj1("I'm obj 1");
        result.setObj2("I'm obj 2");
        return result;
    }

    @KipaskipasCache(updateType = UpdateType.IMMEDIATELY)
    public String sampleCache() {
        logger.debug("hello I'm executed");
        return "hello from okesip";
    }
}
