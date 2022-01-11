package com.mocxa.bloothdevicespeed.device2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mocxa.bloothdevicespeed.BluetoothConstants;
import com.mocxa.bloothdevicespeed.common.AcceptThread;
import com.mocxa.bloothdevicespeed.common.ConnectThread;
import com.mocxa.bloothdevicespeed.device.DeviceCommands;
import com.mocxa.bloothdevicespeed.tools.UtilLogger;
import com.mocxa.bloothdevicespeed.tools.livedata.LiveDataEvent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Niraj on 09-01-2022.
 */
public class BluetoothDevice2Service {

    private UtilLogger log = UtilLogger.with(this);
    private BluetoothSocket mSocket = null;
    BluetoothAdapter mAdapter;
    Handler mReadHandler;
    public Timer timer = new Timer();

    private AcceptThread mSecureAcceptThread = null;

    //    private var mInsecureAcceptThread: AcceptThread? = null
    private ConnectThread mConnectThread = null;
    private Device2SenderThread mDevice2SenderThread = null;
    private Device2ReceiveThread mReceiverService = null;

    private int mState = BluetoothConstants.STATE_NONE;

    private boolean mTransmissionPacketSent = false;


    ConcurrentLinkedQueue<byte[]> packetQueue = new ConcurrentLinkedQueue<byte[]>();

    private MutableLiveData<LiveDataEvent<String>> _mEventErrorMessage = new MutableLiveData<LiveDataEvent<String>>();
    MutableLiveData<LiveDataEvent<String>> mEventErrorMessage = _mEventErrorMessage;
    MediatorLiveData<LiveDataEvent<String>> mEventErrorMessageMediator = new MediatorLiveData<LiveDataEvent<String>>();


    private MutableLiveData<LiveDataEvent<BluetoothSocket>> _mEventConnected = new MutableLiveData<LiveDataEvent<BluetoothSocket>>();
    MutableLiveData<LiveDataEvent<BluetoothSocket>> mEventConnected = _mEventConnected;

    MediatorLiveData<LiveDataEvent<BluetoothSocket>> mEventConnectedMediator = new MediatorLiveData<LiveDataEvent<BluetoothSocket>>();


    private Device2Gate mDevice2Gate = new Device2Gate();
    boolean mIsReceiving = true;


    private HandlerThread mSendThread;
    private TimerTask mPeriodicSender;


    public BluetoothDevice2Service(BluetoothAdapter pAdapter, Handler pHandler) {
        mAdapter = pAdapter;
        mReadHandler = pHandler;

    }

    public void stop() {


        clearAccept();
        clearConnect();

        clearReceiver();
        clearSender();

        if(mSendThread!=null && !mSendThread.isInterrupted()){
            mSendThread.interrupt();
        }

        if(mPeriodicSender!=null){
            mPeriodicSender.cancel();
            mPeriodicSender = null;
        }


/*
        mInsecureAcceptThread?.interrupt();
        mInsecureAcceptThread = null;*/
        mState = BluetoothConstants.STATE_NONE;
    }


    void start() {
        Log.d("BluetoothService", "start");


        clearAccept();
        clearConnect();

        clearReceiver();
        clearSender();

        // Start the thread to listen on a BluetoothServerSocket
//        setUpAccept()
/*        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread!!.start()
        }*/
        // TODO Update UI title
        //
    }


    public void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        // Cancel any thread attempting to make a connection
        if (mState == BluetoothConstants.STATE_CONNECTING) {
            clearConnect();
        }

        // Cancel any thread currently running a connection

        // Cancel any thread currently running a connection
        clearReceiver();
        clearSender();

        setUpConnect(device);
    }

    public void onConnected(BluetoothSocket socket) {
        mSocket = socket;
        mState = BluetoothConstants.STATE_CONNECTED;
        mEventConnected.postValue(new LiveDataEvent(socket));
        if (mConnectThread != null) {
            mConnectThread.setState(mState);

        }
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.mState = mState;
        }

        clearConnect();
        clearAccept();

    }

    private void setUpConnect(BluetoothDevice device) {
        if (mConnectThread == null && mAdapter != null) {
            mConnectThread = new ConnectThread(device, false);
            mEventErrorMessageMediator.addSource(this.mConnectThread.mEventErrorMessage, stringLiveDataEvent -> {
                _mEventErrorMessage.postValue(stringLiveDataEvent);

            });
            mEventConnectedMediator.addSource(this.mConnectThread.mEventConnected, bluetoothSocketLiveDataEvent -> {
                Log.i("BluetoothService: ", "setUpConnect connected");

                onConnected(bluetoothSocketLiveDataEvent.getData());
            });
            Log.i("BluetoothService", "setUpConnect");


            //mSecureAcceptThread.
//            mSecureAcceptThread?.
            mConnectThread.start();
        }
    }

    private boolean setupReceiver() {
        clearReceiver();
        mIsReceiving = true;
        if (mSocket != null) {
            Log.i("Bluetooth Service: ", "setupReceiver");
            mReceiverService = new Device2ReceiveThread(mSocket, mReadHandler, mDevice2Gate);

            if (mState == BluetoothConstants.STATE_CONNECTED) {
                mReceiverService.toggleConnected(true);
            } else {
                mReceiverService.toggleConnected(false);

            }
            mReceiverService.start();
            return true;
        } else {
            return false;
        }
    }


    private boolean setupSender() {
        clearSender();
        if (mSocket != null) {
            Log.i("Bluetooth Service: ", "setupSender");

            mDevice2SenderThread = new Device2SenderThread(mSocket, mDevice2Gate);
            if (mState == BluetoothConstants.STATE_CONNECTED) {
                mDevice2SenderThread.toggleConnected(true);
            } else {
                mDevice2SenderThread.toggleConnected(false);

            }
            mDevice2SenderThread.start();
//       TODO     mDevice2SenderThread.setupHandler();
            return true;
        } else {
            return false;
        }
    }

    public void sendSetup() {
        if (mDevice2SenderThread == null || mSendThread == null) return;

        new Handler(mSendThread.getLooper()).post(() -> {

            String message = DeviceCommands.INITIAL_HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup :  "+ message);

            message = DeviceCommands.HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup :  "+ message);

            message = DeviceCommands.deciData1();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci1:  "+ message);


            message = DeviceCommands.deciData2();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci2:  "+ message);

            message = DeviceCommands.deciData3();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci3:  "+ message);

            message = DeviceCommands.deciData4();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci4:  "+ message);

            message = DeviceCommands.deciData5();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci5:  "+ message);

            message = DeviceCommands.deciData6();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci6:  "+ message);


            message = DeviceCommands.deciData7();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci7:  "+ message);

            message = DeviceCommands.deciData8();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci8:  "+ message);

            message = DeviceCommands.deciData9();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci9:  "+ message);

            message = DeviceCommands.deciData10();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci10:  "+ message);

            message = DeviceCommands.deciData11();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci11:  "+ message);


            message = DeviceCommands.END_ONGOING;
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup end:  "+ message);

        });


    }


    /* ************************************************************************************
     *                                           clear
     */

    private void clearConnect() {
        if (mConnectThread == null) return;
        mConnectThread.interrupt();
        mEventErrorMessageMediator.removeSource(mConnectThread.mEventErrorMessage);
        mConnectThread = null;

    }

    private void clearAccept() {
        if (mSecureAcceptThread == null) return;
        mSecureAcceptThread.interrupt();
        mEventErrorMessageMediator.removeSource(mSecureAcceptThread.mEventErrorMessage);
        mSecureAcceptThread = null;

    }


    private void clearSender() {
        if (mDevice2SenderThread != null) {
            mDevice2SenderThread.interrupt();
        }
//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mDevice2SenderThread = null;

    }

    private void clearReceiver() {
        if (mReceiverService != null)
            mReceiverService.interrupt();
//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mReceiverService = null;

    }

    public int getState() {
        return mState;
    }

    public void stopChat() {
        if (mReceiverService != null) {
            mReceiverService.toggleConnected(false);
            mReceiverService.interrupt();
        }

        clearReceiver();


        if (mDevice2SenderThread != null) {
            mDevice2SenderThread.toggleConnected(false);
            mDevice2SenderThread.interrupt();
        }

        clearSender();

        if(mPeriodicSender!=null){
            mPeriodicSender.cancel();
            mPeriodicSender = null;
            timer.cancel();
        }


    }

    public String getCounterLog() {

        long currentTime = System.currentTimeMillis();

        if (mReceiverService != null) {
            long timeDiff = (currentTime - mReceiverService.getStartTime());
            long byteCounter = mReceiverService.getByteCounter();
            return " Receiving:  \n" +
                    "Rate: " +
                    (byteCounter * 1000) / timeDiff +
                    " \n" +

                    "Period: " +
                    timeDiff +
                    " \n" +
                    "Counter: " +
                    mReceiverService.getCounter() +
                    "  " +
                    mReceiverService.getReadCounter() +
                    " \n" +
                    "BytesCounter: " +
                    mReceiverService.getByteCounter() +
                    " \n";
        }
        return "No result";

    }

    public void resetCounterLog() {

        if (mReceiverService != null) {
            mReceiverService.resetLog();
        }
    }


    public void startEEG() {


        new Handler(mSendThread.getLooper()).post(() -> {
            String message = DeviceCommands.INITIAL_HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("startEEG initial:  "+ message);


            message = DeviceCommands.HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("startEEG HEART_BEAT:  "+ message);


            mPeriodicSender = new TimerTask() {
                @Override
                public void run() {
                    periodicSend();
                }
            };
            timer.schedule(mPeriodicSender, 0l, 100 * 1 * 1);

        });


    }

    public void stopEEG() {


        new Handler(mSendThread.getLooper()).post(() -> {
            mDevice2SenderThread.sendMessage(DeviceCommands.STOP);
            stopChat();


        });


    }

    private void periodicSend(){
        new Handler(mSendThread.getLooper()).post(() -> {
            mDevice2SenderThread.sendMessage(DeviceCommands.HEART_BEAT);
            if (!mTransmissionPacketSent) {
                mDevice2SenderThread.sendMessage(DeviceCommands.TRANSMISSION);
                mTransmissionPacketSent = true;
            }
        });
    }

    public void setupSendReceive() {
        if (mSendThread != null && !mSendThread.isInterrupted()) {
            mSendThread.isInterrupted();
        }

        mSendThread = new HandlerThread("SendThread");
        mSendThread.start();

        setupSender();
        setupReceiver();
    }

    /* *********************************************************************************
     *                          Processing
     */

    private class ProcessingThread extends Thread {

        int processingCounter = 0;
        @Override
        public void run() {
            while (!isInterrupted()) {

                byte[] databuf = packetQueue.poll();
                if(databuf!=null){
                    processingCounter++;
//                   TODO PacketHelper.processBuffer(databuf, )

                }
//
            }
        }
    }


}
