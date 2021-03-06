package com.mocxa.bloothdevicespeed.common;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 01-01-2022.
 */
public class ReceiveThread extends Thread{

    public static final int MESSAGE_READ = 647;
    private final Handler mReadHandler;

    private InputStream mInputStream  = null;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);

    final Object myLock = new Object();

    int mCounter = 0;
    int mReadCounter = 0;
    int mByteCounter = 0;
    long mStartTime = System.currentTimeMillis();

    static final int DEFAULT_BUFFER_SIZE = 2*1024;

    public ReceiveThread(BluetoothSocket pSocket, Handler pReadHandler){
        mReadHandler = pReadHandler;
        try {
            mInputStream = pSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void run() {
//        Log.i("ReceiverService:", "ReceiverService run")
        while (!isInterrupted()) {
//            Log.i("ReceiverService:", "ReceiverService run 2")

            if (mIsConnected.get() && mInputStream != null) {

//                Log.i("ReceiverService:", "ReceiverService run 3")
                synchronized(myLock) {

//                    Log.i("ReceiverService:", "ReceiverService run 4")
                    byte[] buffer = new byte[1024];
                    int bytes = 0;
                    try {
//                        mInputStream.read()
                        if (mCounter == 0) {
                            mStartTime = System.currentTimeMillis();
                        }
                        /*
                        TODO approch 1
                        bytes = mInputStream.read(buffer);
//                        Log.i("ReceiverService:", "ReceiverService $mCounter")
                        if (bytes > 0) {

                            mByteCounter += bytes;
                            mReadHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget();

                            mReadCounter++;
                        }
*/

                        int availableBytes = mInputStream.available();
                        if (availableBytes >0){
                            buffer = new byte[Math.max(availableBytes, DEFAULT_BUFFER_SIZE)];
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(availableBytes, DEFAULT_BUFFER_SIZE));
                            //inputStream.read();

                            // bytes = inputStream.read(buffer);
                            bytes = mInputStream.read(buffer,0,availableBytes);


                            byteArrayOutputStream.write(buffer, 0, bytes);
                            mByteCounter += bytes;
                            mReadCounter++;

                            mReadHandler.obtainMessage(MESSAGE_READ, bytes, -1,
                                    byteArrayOutputStream).sendToTarget();
                        }


                        mCounter++;
                    } catch (IOException e ) {
                        e.printStackTrace();
                    }

                }
            }


        }
    }

    public void toggleConnected(boolean isConnected ) {
        mIsConnected.compareAndSet(!isConnected, isConnected);
        Log.i("SenderService: ", "toggleConnected: ${mIsConnected.get()}");
    }

    public int getByteCounter() {
        return mByteCounter;
    }

    public long getStartTime() {
        return mStartTime;
    }


    public int getCounter() {
        return mCounter;
    }
    public int getReadCounter() {
        return mReadCounter;
    }

    public void resetLog() {
        synchronized(myLock) {
            mByteCounter = 0;
            mCounter = 0;
            mReadCounter = 0;
        }
    }
}
