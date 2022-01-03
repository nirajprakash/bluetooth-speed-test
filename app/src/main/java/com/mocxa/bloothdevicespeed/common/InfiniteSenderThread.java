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

    private OutputStream mOutputStream = null;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);

    final Object myLock = new Object();

    long mStartTime = System.currentTimeMillis();
    int mCounter = 0;
    int mByteCounter = 0;

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

        while (!isInterrupted()) {

//            Log.i("SenderService:", "SenderService run 1")
            if (mIsConnected.get() && mOutputStream!=null) {

//                Log.i("SenderService:", "SenderService run 2")
                synchronized (myLock) {

//                    Log.i("SenderService:", "SenderService run 3")
                    try {

                        if(mCounter == 0){
                            mStartTime = System.currentTimeMillis();
                        }


//                        Log.i("SenderService:", "SenderService $mCounter")
                        mCounter++;
                        byte[] bytes = "  Message: $mCounter".getBytes(Charset.defaultCharset());
                        this.mByteCounter += bytes.length;
                        mOutputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }


        }
    }

    public void toggleConnected(boolean isConnected) {
        mIsConnected.compareAndSet(!isConnected, isConnected);
        Log.i("SenderService: ", "toggleConnected: ${mIsConnected.get()}");
    }

    public long getStartTime() {
        return mStartTime;
    }

    public int getCounter() {
        return mCounter;
    }


    public int getByteCounter() {
        return mByteCounter;
    }


    public void resetLog() {
        synchronized(myLock) {
            mByteCounter = 0;
            mCounter = 0;
        }
    }
}
