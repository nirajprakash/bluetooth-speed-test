package com.mocxa.bloothdevicespeed.device2;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.mocxa.bloothdevicespeed.tools.UtilLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 09-01-2022.
 */

// https://stackoverflow.com/questions/4876329/inputstream-available-doesnt-work
// https://stackoverflow.com/questions/5826198/inputstream-available-is-0-always#
// reader https://social.msdn.microsoft.com/Forums/en-US/f559d5c7-b195-462e-bec2-dfb28bf1c7e4/bluetooth-socket-is-blocked-on-read?forum=xamarinandroid
// reader https://stackoverflow.com/questions/66299558/android-bluetooth-socket-has-incomplete-buffer-after-restart
// write close http://www.java2s.com/example/android/bluetooth/handles-closing-the-bluetooth-socket-and-flushescloses-output-stream.html
public class Device2ReceiveThread extends Thread {

    private static final int READ_METHOD_SIMPLE = 286;
    private static final int READ_METHOD_ONE_BY_ONE = 288;
    private static final int READ_METHOD_AVAILABLE = 296;
    private static final int READ_METHOD_BUFFER = 731;
    private static final int READ_METHOD_BUFFER_ONE_BY_ONE = 733;
    private static final int READ_METHOD_BUFFER_PACKET = 736;
    private static final int READ_METHOD_BUFFER_PACKET_AVAILABLE = 738;

    private UtilLogger log = UtilLogger.with(this);
    public static final int MESSAGE_READ = 647;


    private BufferedInputStream mBufferedInputStream;
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


    int readMethodApproch = READ_METHOD_ONE_BY_ONE;

    public Device2ReceiveThread(BluetoothSocket pSocket, Handler pReadHandler, Device2Gate device2Gate) {
        mReadHandler = pReadHandler;
        try {
            mInputStream = pSocket.getInputStream();

            if (readMethodApproch == READ_METHOD_BUFFER ||
                    readMethodApproch == READ_METHOD_BUFFER_ONE_BY_ONE ||
                    readMethodApproch == READ_METHOD_BUFFER_PACKET ||
                    readMethodApproch == READ_METHOD_BUFFER_PACKET_AVAILABLE
            ) {
                mBufferedInputStream = new BufferedInputStream(mInputStream);
            }
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
                    if (mCounter == 0) {
                        mStartTime = System.currentTimeMillis();
                    }
                    if (mDevice2Gate.getReadActive().get()) {
                        try {
//                            log.i( "ReceiverService run 4");

//                        mInputStream.read()


                            if (readMethodApproch == READ_METHOD_SIMPLE) {
                                readBySimple();
                            } else if(readMethodApproch == READ_METHOD_ONE_BY_ONE) {
                                readByOneByOne();
                            }else if (readMethodApproch == READ_METHOD_AVAILABLE) {
                                readByAvailable();
                            } else if (readMethodApproch == READ_METHOD_BUFFER) {
                                readByBuffer();
                            } else if (readMethodApproch == READ_METHOD_BUFFER_ONE_BY_ONE) {
                                readByBufferOneByOne();
                            } else if (readMethodApproch == READ_METHOD_BUFFER_PACKET) {
                                readByBufferPacket();
                            }else if (readMethodApproch == READ_METHOD_BUFFER_PACKET_AVAILABLE) {
                                readByBufferPacketAvailable();
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
            mDevice2Gate.holdRead();
        }
    }

    void readByOneByOne() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        int byteLength = 0;
        int byteInt;
//        int zeroD = -1;
        boolean end =false;
        while (!end) {



            byteInt = mInputStream.read();
            if(byteInt == 0x0d){
                end = true;
                byteArrayOutputStream.write(byteInt);
                byteLength++;
            }else if(byteInt == -1){
                end =  true;
            }else{
                byteArrayOutputStream.write(byteInt);
                byteLength++;
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

        mDevice2Gate.holdRead();
    }

    void readByAvailable() throws IOException {
        int availableBytes = mInputStream.available();
        if (availableBytes > 0) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(availableBytes, DEFAULT_BUFFER_SIZE));
            int writeOffset = 0;
            int byteLength = 0;
            while (availableBytes > 0) {
//                byte[] buffer = new byte[availableBytes];

                byte[] buffer = new byte[Math.max(availableBytes, DEFAULT_BUFFER_SIZE)];


                int bytes = mInputStream.read(buffer, 0, availableBytes);
                byteLength += bytes;
                byteArrayOutputStream.write(buffer, writeOffset, bytes);
                writeOffset += bytes;

                log.i("readByAvailable run 6: " + System.currentTimeMillis() + " " + availableBytes);
                availableBytes = mInputStream.available();

            }
            mByteCounter += byteLength;
            mReadCounter++;
            log.i("readByAvailable run 5");

            mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                    byteArrayOutputStream).sendToTarget();

            mDevice2Gate.holdRead();
        } else {
//                                log.i( "ReceiverService run 7");
            mDevice2Gate.holdRead();
        }
    }


    /* **********************************************************************************************
     *                                          useless
     */

    private void readByBuffer() throws IOException {

//        log.i("readByBuffer");
        int availableBytes = mBufferedInputStream.available();
        if (availableBytes > 0) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(availableBytes, DEFAULT_BUFFER_SIZE));
            int writeOffset = 0;
            int byteLength = 0;
            while (availableBytes > 0) {
//                byte[] buffer = new byte[availableBytes];
                byte[] buffer = new byte[Math.max(availableBytes, DEFAULT_BUFFER_SIZE)];


                int bytes = mBufferedInputStream.read(buffer, 0, availableBytes);
                byteLength += bytes;
                byteArrayOutputStream.write(buffer, writeOffset, bytes);
                writeOffset += bytes;

                log.i("readByBuffer run 6: " + System.currentTimeMillis() + " " + availableBytes);
                availableBytes = mBufferedInputStream.available();

            }
            mByteCounter += byteLength;
            mReadCounter++;
            log.i("readByBuffer run 5");

            mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                    byteArrayOutputStream).sendToTarget();

            mDevice2Gate.holdRead();
        } else {
//                                log.i( "ReceiverService run 7");
            mDevice2Gate.holdRead();
        }
    }

    private void readByBufferOneByOne() throws IOException {
//        int availableBytes = mBufferedInputStream.available();
//        if (availableBytes > 0) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        int byteLength = 0;
        int byteInt;
        while ((byteInt = mBufferedInputStream.read()) != -1) {

            byteArrayOutputStream.write(byteInt);
            byteLength++;


        }
        mByteCounter += byteLength;
        mReadCounter++;


        if (byteLength > 0) {
            log.i("readByBufferOneByOne run 5" + System.currentTimeMillis() + " " + byteLength);
//                log.i( "readByBuffer run 6: "+ System.currentTimeMillis() + " "  + availableBytes);
            mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                    byteArrayOutputStream).sendToTarget();

        }

        mDevice2Gate.holdRead();
//        } else {
//                                log.i( "ReceiverService run 7");
//            mDevice2Gate.holdRead();
//        }
    }

    private void readByBufferPacket() throws IOException {
//        int availableBytes = mBufferedInputStream.available();
//        if (availableBytes > 0) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        //inputStream.read();

        // bytes = inputStream.read(buffer);
        int bytes = mBufferedInputStream.read(buffer);
        int writeOffset = 0;


        int byteLength = 0;
//        while (bytes>0) {


        byteLength += bytes;
        byteArrayOutputStream.write(buffer, writeOffset, bytes);


//        }
        mByteCounter += byteLength;
        mReadCounter++;


        if (byteLength > 0) {
            log.i("readByBufferPacket run 5" + System.currentTimeMillis() + " " + byteLength);
            mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                    byteArrayOutputStream).sendToTarget();

        } else {
            mDevice2Gate.holdRead();
        }


//        } else {
//                                log.i( "ReceiverService run 7");
//            mDevice2Gate.holdRead();
//        }
    }

    private void readByBufferPacketAvailable() throws IOException {
        int availableBytes = mBufferedInputStream.available();
        if (availableBytes > 0) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
//            mBufferedInputStream.read();

            // bytes = inputStream.read(buffer);
            int bytes = mBufferedInputStream.read(buffer, 0, availableBytes);
            int writeOffset = 0;


            int byteLength = 0;
//        while (bytes>0) {


            byteLength += bytes;
            byteArrayOutputStream.write(buffer, 0, bytes);


//        }
            mByteCounter += byteLength;
            mReadCounter++;

//            log.i("readByBufferPacketAvailable run 5" + System.currentTimeMillis() + " " + byteLength);
            mReadHandler.obtainMessage(MESSAGE_READ, byteLength, -1,
                    byteArrayOutputStream).sendToTarget();

        } else {
            mDevice2Gate.holdRead();
        }


//        } else {
//                                log.i( "ReceiverService run 7");
//            mDevice2Gate.holdRead();
//        }
    }

}
