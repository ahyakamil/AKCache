package com.kipaskipas.cache.sample;

import com.kipaskipas.cache.annotation.KipaskipasCache;

public class Sample {
    @KipaskipasCache(key = "mantap")
    public String sampleCache(String paramText) {
        return "hello from sample cache " + paramText;
    }

    @KipaskipasCache(key = "okesip")
    public String sampleCache() {
        return "hello from okesip";
    }
}
