package com.mocxa.bloothdevicespeed.tools.livedata;

/**
 * Created by Niraj on 01-01-2022.
 */
public class LiveDataEvent<T> {

    private Boolean isConsumed = false;

    T data;

    public LiveDataEvent(T pData) {
        this.data = pData;
    }

    public T getData() {
        return data;
    }

    public T getDataIfNotConsumed(){
        if(isConsumed){
            return null;
        }else{
            isConsumed = true;
            return data;
        }

    }
}
