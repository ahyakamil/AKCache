package com.kipaskipas.cache;

import com.kipaskipas.cache.sample.Sample;
import com.kipaskipas.cache.sample.SampleDto;
import org.junit.jupiter.api.Test;

class KipaskipasCacheTest {
    public void setupConnection() {
        KipaskipasCacheSetup.setup("localhost", 6379, "", "");
    }

    @Test
    public void testAnnotation() {
        setupConnection();
        Sample sample = new Sample();
        sample.sampleCache("mantap");
        sample.sampleCache();
    }
}