package com.ahyakamil.AKCache;

import com.ahyakamil.AKCache.sample.Sample;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CaseTest {
    @Before
    public void setup() {
        AKCacheSetup.setupConnection("localhost", 6379, "", "");
    }

    @Test
    public void case1() {
        Sample sample = new Sample();
        String result = sample.case1();
        assertEquals(result, "What a beautiful day!");
    }

    @Test
    public void case2() {
        Sample sample = new Sample();
        String result = sample.case2();
        assertEquals(result, "What a beautiful day!");
    }
}
