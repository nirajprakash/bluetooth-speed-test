package com.mocxa.bloothdevicespeed.tools.livedata;

import androidx.lifecycle.Observer;

/**
 * Created by Niraj on 01-01-2022.
 */
public class LiveDataObserver<T> implements Observer<LiveDataEvent<T>> {

    LiveDataObserverCallback<T> callback;
    public LiveDataObserver(LiveDataObserverCallback<T> pCallback){
        callback = pCallback;
    }

    @Override
    public void onChanged(LiveDataEvent<T> tLiveDataEvent) {
       if(tLiveDataEvent!=null){
          T t =  tLiveDataEvent.getDataIfNotConsumed();
           if(t!=null){
               callback.onChanged(t);

           }
       }


    }
}
