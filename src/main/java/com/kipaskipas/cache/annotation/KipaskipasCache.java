package com.kipaskipas.cache.annotation;

import com.kipaskipas.cache.constant.UpdateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KipaskipasCache {
    UpdateType updateType();
}
