package com.kipaskipas.cache;

import com.kipaskipas.cache.sample.Sample;
import org.junit.jupiter.api.Test;

class KipaskipasCacheTest {
    public void setupConnection() {
        KipaskipasCacheSetup.setup("localhost", 6379, "", "");
    }

    @Test
    public void testAnnotation() {
        setupConnection();
        Sample sample = new Sample();
        String hello = sample.sampleCache("mantap");
        System.out.println("ini adalah " + hello);
        System.out.println("ini dari sebelah " + sample.sampleCache());
    }
}