package com.mocxa.bloothdevicespeed.mobile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mocxa.bloothdevicespeed.BluetoothConstants;
import com.mocxa.bloothdevicespeed.common.AcceptThread;
import com.mocxa.bloothdevicespeed.common.ConnectThread;
import com.mocxa.bloothdevicespeed.common.InfiniteSenderThread;
import com.mocxa.bloothdevicespeed.common.ReceiveThread;
import com.mocxa.bloothdevicespeed.tools.livedata.LiveDataEvent;


/**
 * Created by Niraj on 01-01-2022.
 */
public class BluetoothMobileService {

    private BluetoothSocket mSocket=null;
    BluetoothAdapter mAdapter;
    Handler mReadHandler;

    private AcceptThread mSecureAcceptThread = null;

    //    private var mInsecureAcceptThread: AcceptThread? = null
    private ConnectThread mConnectThread = null;
    private InfiniteSenderThread mSenderThread = null;
    private ReceiveThread mReceiverService = null;

    private int mState = BluetoothConstants.STATE_NONE;


    private MutableLiveData<LiveDataEvent<String>> _mEventErrorMessage = new MutableLiveData<LiveDataEvent<String>>();
    MutableLiveData<LiveDataEvent<String>> mEventErrorMessage = _mEventErrorMessage;


    MediatorLiveData<LiveDataEvent<String>> mEventErrorMessageMediator = new MediatorLiveData<LiveDataEvent<String>>();


    private MutableLiveData<LiveDataEvent<BluetoothSocket>> _mEventConnected = new MutableLiveData<LiveDataEvent<BluetoothSocket>>();
    MutableLiveData<LiveDataEvent<BluetoothSocket>> mEventConnected = _mEventConnected;
    MediatorLiveData<LiveDataEvent<BluetoothSocket>> mEventConnectedMediator = new MediatorLiveData<LiveDataEvent<BluetoothSocket>>();


    boolean mIsReceiving = false;

    public BluetoothMobileService(BluetoothAdapter pAdapter, Handler pHandler) {
        mAdapter = pAdapter;
        mReadHandler = pHandler;

    }

    public void stop() {


        clearAccept();
        clearConnect();

        clearReceiver();
        clearSender();

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


public void setUpAccept() {

        if (mSecureAcceptThread == null && mAdapter != null) {
            mSecureAcceptThread = new AcceptThread(mAdapter, true);
            mEventErrorMessageMediator.addSource(this.mSecureAcceptThread.mEventErrorMessage, stringLiveDataEvent -> {
                _mEventErrorMessage.postValue(stringLiveDataEvent);

            });
            mEventConnectedMediator.addSource(this.mSecureAcceptThread.mEventConnected, bluetoothSocketLiveDataEvent -> {
                onConnected(bluetoothSocketLiveDataEvent.getData());

            }) ;
            mState = mSecureAcceptThread.mState;

            Log.i("BluetoothService", "setUpAccept");
            //mSecureAcceptThread.
//            mSecureAcceptThread?.
            mSecureAcceptThread.start();
        }
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

    public void  onConnected(BluetoothSocket socket) {
        mSocket = socket;
        mState = BluetoothConstants.STATE_CONNECTED;
        mEventConnected.postValue(new LiveDataEvent(socket));
        if(mConnectThread!=null){
            mConnectThread.setState(mState);

        }
        if(mSecureAcceptThread!=null){
            mSecureAcceptThread.mState = mState;
        }

        clearConnect();
        clearAccept();

    }

    private void setUpConnect(BluetoothDevice device) {
        if (mConnectThread == null && mAdapter != null) {
            mConnectThread = new ConnectThread(device, true);
            mEventErrorMessageMediator.addSource(this.mConnectThread.mEventErrorMessage, stringLiveDataEvent -> {
                _mEventErrorMessage.postValue(stringLiveDataEvent);

            }) ;
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

    public boolean setupReceiver(){
        clearReceiver();
        clearSender();
        mIsReceiving = true;
        if (mSocket != null) {
            Log.i("Bluetooth Service: ", "setupReceiver");
            mReceiverService = new ReceiveThread(mSocket, mReadHandler);

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


    public boolean setupSender() {
        clearSender();
        clearReceiver();
        mIsReceiving = false;
        if (mSocket != null) {
            Log.i("Bluetooth Service: ", "setupSender");

            mSenderThread = new InfiniteSenderThread(mSocket);
            if (mState == BluetoothConstants.STATE_CONNECTED) {
                mSenderThread.toggleConnected(true);
            } else {
                mSenderThread.toggleConnected(false);

            }
            mSenderThread.start();
            return true;
        } else {
            return false;
        }
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
        if (mSenderThread != null)
            mSenderThread.interrupt();
//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mSenderThread = null;

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
        if (mIsReceiving) {
            if (mReceiverService != null) {
                mReceiverService.toggleConnected(false);
                mReceiverService.interrupt();
            }

            clearReceiver();
        } else {

            if (mSenderThread != null) {
                mSenderThread.toggleConnected(false);
                mSenderThread.interrupt();
            }

            clearSender();
        }
    }

    public String getCounterLog() {

        long currentTime = System.currentTimeMillis();

        if (!mIsReceiving && mSenderThread != null) {
            long timeDiff = (currentTime - mSenderThread.getStartTime()) / 1000;
            return " Sending:  \n" +
                    "Period: $timeDiff \n" +
                    "Counter: ${mSenderThread!!.getCounter()} \n";
        } else if (mReceiverService != null) {
            long timeDiff = (currentTime - mReceiverService.getStartTime()) / 1000;
            return " Receiving:  \n" +
                    "Period: $timeDiff \n" +
                    "Counter: ${mReceiverService!!.getCounter()} \n" +
                    "BytesCounter: ${mReceiverService!!.getByteCounter()} \n";
        }
        return "No result";

    }

}
