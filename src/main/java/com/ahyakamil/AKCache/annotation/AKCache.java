package com.ahyakamil.AKCache.annotation;

import com.ahyakamil.AKCache.constant.UpdateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AKCache {
    UpdateType updateType() default UpdateType.SMART;
    Class serializeClass() default Object.class;
}
