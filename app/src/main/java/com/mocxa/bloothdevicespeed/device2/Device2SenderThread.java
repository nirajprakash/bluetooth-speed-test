package com.mocxa.bloothdevicespeed.device2;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mocxa.bloothdevicespeed.tools.UtilLogger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 03-01-2022.
 */
public class Device2SenderThread extends Thread {

    private static final int WRITE_METHOD_SIMPLE = 286;
    private static final int WRITE_METHOD_BUFFER = 731;
    private final HandlerThread mHandlerThread;

    private UtilLogger log = UtilLogger.with(this);
    private static final int MESSAGE_SEND = 487;

    private  BufferedOutputStream mBufferedOutputStream;
    BluetoothSocket mSocket;

    private OutputStream mOutputStream = null;
    private AtomicBoolean mIsConnected = new AtomicBoolean(false);

    private ConcurrentLinkedQueue<String> mSendMessagesQueue = new ConcurrentLinkedQueue<>();

    final Object myLock = new Object();

    long mStartTime = System.currentTimeMillis();
    int mCounterLog = 0;
     int mWriteCounterLog = 0;

     int writeMethodApproch = WRITE_METHOD_BUFFER;


// TODO   HandlerThread mHandlerThread = null;
// TODO  private Handler mReaderHandler;

    private Device2Gate mDevice2Gate;


    public Device2SenderThread(BluetoothSocket pSocket, Device2Gate device2Gate, HandlerThread handlerThread) {
        mSocket = pSocket;
        this.mHandlerThread = handlerThread;
        try {
            mOutputStream = mSocket.getOutputStream();

            if(writeMethodApproch == WRITE_METHOD_BUFFER){
                mBufferedOutputStream = new BufferedOutputStream(mOutputStream);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        mDevice2Gate  = device2Gate;

    }


    @Override
    public void run() {
        while (!isInterrupted()) {

            if (mDevice2Gate.getWriteActive().get()) {

                //synchronized (mDevice2Gate.getMyLock()){

                    // TODO add check for ack and nack

                    if(!(mDevice2Gate.getAck().get() & mDevice2Gate.getNack().get())){
                        String pollMessage = mSendMessagesQueue.poll();
                        if(pollMessage!=null){


                            write(pollMessage);
                            mWriteCounterLog++;
                            mDevice2Gate.decrementWriteCounter(mHandlerThread);
                        }else{
                            mDevice2Gate.holdWrite();
                        }
                    }else{
                        mDevice2Gate.holdWrite();
                    }


                mCounterLog++;
            }else{
                if(mSendMessagesQueue.peek()!=null){
                    mDevice2Gate.enableWrite();
                }
            }

        }

    }

    public void sendMessage(String message){
        synchronized (myLock){
            mSendMessagesQueue.add(message);
//            log.i("send Message: "+ message);

        }
/*        if(mReaderHandler!=null){
            mReaderHandler.obtainMessage(MESSAGE_SEND, -1, -1 , message).sendToTarget();
        }*/
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
            if(writeMethodApproch == WRITE_METHOD_BUFFER){
                mBufferedOutputStream.write(by);
                mBufferedOutputStream.flush();
//                log.i("writing Message 1: "+ System.currentTimeMillis() + " ");

//               Doubt mBufferedOutputStream.flush();
            }else {
                mOutputStream.write(by);
//                log.i("writing Message 2: "+ System.currentTimeMillis() + " ");
                mOutputStream.flush();
//                log.i("writing Message 3: "+ System.currentTimeMillis() + " ");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toggleConnected(boolean isConnected) {
        mIsConnected.compareAndSet(!isConnected, isConnected);
        Log.i("SenderService: ", "toggleConnected: ${mIsConnected.get()}");
    }

    /*public void interrupt(){
        if(mHandlerThread!=null && !mHandlerThread.isInterrupted()){
            mHandlerThread.interrupt();
        }

    }*/

    public void resetLog() {
        synchronized (myLock) {
            mCounterLog = 0;
            mWriteCounterLog = 0;
        }
    }


}
