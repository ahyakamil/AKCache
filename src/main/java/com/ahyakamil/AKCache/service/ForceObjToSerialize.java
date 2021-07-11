package com.ahyakamil.AKCache.service;

import java.io.Serializable;

class ForceObjToSerialize<T> extends ObjNotSerialize<T> implements Serializable {
    private static final long serialVersionUID = 11111L;

    public ForceObjToSerialize(T valueObj) {
        super(valueObj);
    }

    public ForceObjToSerialize() {
    }

    @Override
    public T getValueObj() {
        return super.getValueObj();
    }
}

class ObjNotSerialize<T> {
    public T getValueObj() {
        return valueObj;
    }

    T valueObj;

    public ObjNotSerialize(T valueObj) {
        this.valueObj = valueObj;
    }

    public ObjNotSerialize() {

    }
}