package com.mocxa.bloothdevicespeed.eeg.device;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.mocxa.bloothdevicespeed.device2.Device2Gate;
import com.mocxa.bloothdevicespeed.tools.UtilLogger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 17-01-2022.
 */
public class EEGDeviceReceiveThread extends Thread {

    private static final int READ_METHOD_SIMPLE = 286;
    private static final int READ_METHOD_ONE_BY_ONE = 288;

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
    private EEGDeviceGate mDeviceGate;


    int readMethodApproch = READ_METHOD_ONE_BY_ONE;

    public EEGDeviceReceiveThread(BluetoothSocket pSocket, Handler pReadHandler,
                                  EEGDeviceGate deviceGate) {
        mReadHandler = pReadHandler;
        try {
            mInputStream = pSocket.getInputStream();


        } catch (IOException e) {
            e.printStackTrace();
        }
        mDeviceGate = deviceGate;

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
                    if (mCounter == 0) {
                        mStartTime = System.currentTimeMillis();
                    }
                    if (mDeviceGate.getReadActive().get()) {
                        try {
//                            log.i( "ReceiverService run 4");

//                        mInputStream.read()


                            if (readMethodApproch == READ_METHOD_SIMPLE) {
                                readBySimple();
                            } else if(readMethodApproch == READ_METHOD_ONE_BY_ONE) {
                                readByOneByOne();
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mDeviceGate.enableRead();
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

    /* ***************************************************************************************
     *                                             read
     */

    void readBySimple() throws IOException {
        int availableBytes = mInputStream.available();
        if (availableBytes > 0) {
            byte[] buffer = new byte[Math.max(availableBytes, DEFAULT_BUFFER_SIZE)];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(availableBytes, DEFAULT_BUFFER_SIZE));
            //inputStream.read();

//              mInputStream.read(buffer);
            int bytes = mInputStream.read(buffer, 0, availableBytes);


            if (bytes > 0) {
//                byteArrayOutputStream.write(2);
                byteArrayOutputStream.write(buffer, 0, bytes);
                mByteCounter += bytes;
                mReadCounter++;
                log.i("readBySimple run 6: " + System.currentTimeMillis() + " " + bytes);

                mReadHandler.obtainMessage(MESSAGE_READ, bytes, -1,
                        byteArrayOutputStream).sendToTarget();
            }

        } else {
            mDeviceGate.holdRead();
        }
    }

    void readByOneByOne() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        int byteLength = 0;
        int byteInt;
//        int zeroD = -1;
        boolean end =false;
        int availableBytes = mInputStream.available();
        if (availableBytes > 0) {
            while (!end) {

                byteInt = mInputStream.read();
                if (byteInt == 0x0d) {
                    end = true;
                    byteArrayOutputStream.write(byteInt);
                    byteLength++;
                } else if (byteInt == -1) {
                    end = true;
                } else {

                    byteArrayOutputStream.write(byteInt);
                    byteLength++;
                }

//            if(System.currentTimeMillis()%1000 == 0){
////                log.i("reading: "+ byteLength);
//            }

            }
        }

        mByteCounter += byteLength;
        mReadCounter++;


        if (byteLength > 0) {
            log.i("readByOneByOne run 5" + System.currentTimeMillis() + " " + byteLength);
//                log.i( "readByBuffer run 6: "+ System.currentTimeMillis() + " "  + availableBytes);
            mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                    byteArrayOutputStream).sendToTarget();

        }

        mDeviceGate.holdReadForced();
    }


}
