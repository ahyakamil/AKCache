package com.ahyakamil.AKCache.sample;

import com.ahyakamil.AKCache.annotation.AKCache;
import com.ahyakamil.AKCache.constant.UpdateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sample {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @AKCache(updateType = UpdateType.SMART)
    public SampleDto sampleCache(String paramText) {
        SampleDto result = new SampleDto();
        logger.debug("hello I'm executed");
        result.setObj1("I'm obj 1");
        result.setObj2("I'm obj 2");
        return result;
    }

    @AKCache(updateType = UpdateType.FETCH)
    public String sampleCache() {
        logger.debug("hello I'm executed");
        return "hello from okesip";
    }
}
