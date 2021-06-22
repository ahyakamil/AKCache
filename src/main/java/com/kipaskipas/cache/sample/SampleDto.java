package com.kipaskipas.cache.sample;

import java.io.Serializable;

public class SampleDto implements Serializable {
    private static final long serialVersionUID = 1L;

    String obj1;
    String obj2;

    public String getObj1() {
        return obj1;
    }

    public void setObj1(String obj1) {
        this.obj1 = obj1;
    }

    public String getObj2() {
        return obj2;
    }

    public void setObj2(String obj2) {
        this.obj2 = obj2;
    }
}
