package com.mocxa.bloothdevicespeed.tools;

import android.util.Log;

/**
 * Created by niraj on 12-09-2018.
 */
public class UtilLogger {
    String TAG = null; //Class.class.getSimpleName();

    private UtilLogger(String tag) {
        this.TAG = tag;
    }

    public static UtilLogger with(Object object){

        String tag = object.getClass().getSimpleName();


        return new UtilLogger(tag);
    }
    public static UtilLogger forTag(String tag){
        return new UtilLogger(tag);
    }
    public void e(String message){
        Log.e(TAG, message);
    }


    public void v(String message){
        Log.v(TAG, message);
    }

    public void i(String message){
        Log.i(TAG, message);
    }

    public void w(String message){
        Log.w(TAG, message);
    }

    public void d(String message){
        Log.d(TAG, message);
    }

}
