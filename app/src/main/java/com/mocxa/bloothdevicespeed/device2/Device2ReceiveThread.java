package com.mocxa.bloothdevicespeed.device2;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.mocxa.bloothdevicespeed.tools.UtilLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 09-01-2022.
 */
public class Device2ReceiveThread extends Thread {

    private UtilLogger log = UtilLogger.with(this);
    public static final int MESSAGE_READ = 647;
    private final Handler mReadHandler;

    private InputStream mInputStream = null;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);

    final Object myLock = new Object();

    int mCounter = 0;
    int mReadCounter = 0;
    int mByteCounter = 0;
    long mStartTime = System.currentTimeMillis();

    static final int DEFAULT_BUFFER_SIZE = 2 * 1024;
    private Device2Gate mDevice2Gate;

    public Device2ReceiveThread(BluetoothSocket pSocket, Handler pReadHandler, Device2Gate device2Gate) {
        mReadHandler = pReadHandler;
        try {
            mInputStream = pSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDevice2Gate = device2Gate;

    }

    @Override
    public void run() {
//        Log.i("ReceiverService:", "ReceiverService run")
        while (!isInterrupted()) {
//            Log.i("ReceiverService:", "ReceiverService run 2")

            if (mIsConnected.get() && mInputStream != null) {

//                log.i( "ReceiverService run 3");
                synchronized (myLock) {

//                    Log.i("ReceiverService:", "ReceiverService run 4")

                    if (mDevice2Gate.getReadActive().get()) {
                        try {
//                            log.i( "ReceiverService run 4");

//                        mInputStream.read()
                            if (mCounter == 0) {
                                mStartTime = System.currentTimeMillis();
                            }

                            int availableBytes = mInputStream.available();
                            if (availableBytes > 0) {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(availableBytes, DEFAULT_BUFFER_SIZE));
                                int writeOffset = 0;
                                int byteLength = 0;
                                while (availableBytes > 0) {
                                    byte[] buffer = new byte[availableBytes];
                                    int bytes = mInputStream.read(buffer, 0, availableBytes);
                                    byteLength += bytes;
                                    byteArrayOutputStream.write(buffer, writeOffset, bytes);
                                    writeOffset += bytes;
                                    availableBytes = mInputStream.available();

                                    log.i( "ReceiverService run 6");
                                }
                                mByteCounter += byteLength;
                                mReadCounter++;
                                log.i( "ReceiverService run 5");

                                mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                                        byteArrayOutputStream).sendToTarget();

                                mDevice2Gate.holdRead();
                            } else {
//                                log.i( "ReceiverService run 7");
                                mDevice2Gate.holdRead();
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mDevice2Gate.enableRead();
                    }

                    mCounter++;
                }
            }


        }
    }


    public void toggleConnected(boolean isConnected) {
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
        synchronized (myLock) {
            mByteCounter = 0;
            mCounter = 0;
            mReadCounter = 0;
        }
    }
}
