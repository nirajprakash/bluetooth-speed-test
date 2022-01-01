package com.mocxa.bloothdevicespeed.common;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 01-01-2022.
 */
public class InfiniteSenderThread extends Thread {

    BluetoothSocket mSocket;

    private OutputStream mOutputStream  = null;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);

    Object myLock = new Object();

    long mStartTime = System.currentTimeMillis();
    int mCounter = 0;

    public InfiniteSenderThread(BluetoothSocket pSocket) {
        mSocket = pSocket;
        try {
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        //        Log.i("SenderService:", "SenderService run")
        mStartTime = System.currentTimeMillis();
        while (!isInterrupted()) {

//            Log.i("SenderService:", "SenderService run 1")
            if(mIsConnected.get()){

//                Log.i("SenderService:", "SenderService run 2")
                synchronized(myLock) {

//                    Log.i("SenderService:", "SenderService run 3")
                    try {


//                        Log.i("SenderService:", "SenderService $mCounter")
                        mCounter++;
                        mOutputStream.write("  Message: $mCounter".getBytes(Charset.defaultCharset()));
                    } catch (IOException e ) {
                        e.printStackTrace();
                    }

                }
            }


        }
    }

    public void toggleConnected(boolean isConnected){
        mIsConnected.compareAndSet(!isConnected, isConnected);
        Log.i("SenderService: ", "toggleConnected: ${mIsConnected.get()}");
    }

    public long getStartTime() {
        return mStartTime;
    }

    public int getCounter(){
        return  mCounter;
    }


}
