package com.mocxa.bloothdevicespeed.device;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mocxa.bloothdevicespeed.common.ReceiveThread;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 03-01-2022.
 */
public class DeviceSender {

    private static final int MESSAGE_SEND = 487;
    BluetoothSocket mSocket;

    private OutputStream mOutputStream = null;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);

    final Object myLock = new Object();

    long mStartTime = System.currentTimeMillis();
    int mCounter = 0;
    int mByteCounter = 0;

    HandlerThread mHandlerThread = null;
    private Handler mReaderHandler;


    public DeviceSender(BluetoothSocket pSocket) {
        mSocket = pSocket;
        try {
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setupHandler(){
        mHandlerThread = new HandlerThread("SenderHandlerThread");
        mHandlerThread.start();
        mReaderHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
            /*    Log.d(
                        "MainActivity",
                        "handleMessage " + msg.what + " in " +
                                (msg.obj != null)
                );*/
                if (msg.what == MESSAGE_SEND && mHandlerThread!=null && !mHandlerThread.isInterrupted()) {
                    /*Log.d(
                            "MainActivity",
                            "handleMessage in"
                    );*/
                    Object objData = msg.obj;

                    if (objData != null) {
//                        Log.i("mainActivity: ", "mCurrentMessage 2");
                        String message = (String) objData;
                        write(message);


                    }
                }

            /* Log.d(
                    "MainActivity",
                    "handleMessage " + msg.what.toString() + " in " + Thread.currentThread()
                )*/
            }

        };
    }

    private void clear() {
        if (mHandlerThread != null && !mHandlerThread.isInterrupted())
            mHandlerThread.interrupt();
//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mHandlerThread = null;

    }

    public void sendMessage(String message){
        if(mReaderHandler!=null){
            mReaderHandler.obtainMessage(MESSAGE_SEND, -1, -1 , message).sendToTarget();
        }
    }

    private void write(String buffer){
        String[] buffers = buffer.split(",");
        int[] inum = new int[buffers.length];
        int i = 0;
        for (String bufferss : buffers) {
            inum[i] = Integer.parseInt(bufferss);
            i++;
        }

        char[] test1 = new char[inum.length];
        for (int j = 0; j < inum.length; j++) {
            test1[j] = (char) inum[j];
        }

        char[] test = test1;
        byte[] by = new byte[test.length];
        for (int k = 0; k < test.length; k++) {
//            TODO not used outputStream1.write(test[k]);
            by[k] = (byte) test[k];
        }
        try {
            mOutputStream.write(by);
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toggleConnected(boolean isConnected) {
        mIsConnected.compareAndSet(!isConnected, isConnected);
        Log.i("SenderService: ", "toggleConnected: ${mIsConnected.get()}");
    }

    public void interrupt(){
        if(mHandlerThread!=null && !mHandlerThread.isInterrupted()){
            mHandlerThread.interrupt();
        }

    }


}
