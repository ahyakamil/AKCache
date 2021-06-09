package com.kipaskipas.cache;

import com.kipaskipas.cache.sample.Sample;
import org.junit.jupiter.api.Test;

class KipaskipasCacheTest {
    public void setupConnection() {
        KipaskipasCacheSetup.setup("mabes132@localhost:6379");
    }

    @Test
    public void testAnnotation() {
        setupConnection();
        Sample sample = new Sample();
        String hello = sample.sampleCache("mantap");
        sample.sampleCache();
    }
}