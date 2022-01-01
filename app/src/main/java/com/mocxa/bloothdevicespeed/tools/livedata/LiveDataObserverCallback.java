package com.mocxa.bloothdevicespeed.tools.livedata;

/**
 * Created by Niraj on 01-01-2022.
 */
@FunctionalInterface
public interface LiveDataObserverCallback<T> {
    public void onChanged(T data);
}
