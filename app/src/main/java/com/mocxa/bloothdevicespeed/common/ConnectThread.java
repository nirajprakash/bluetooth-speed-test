package com.mocxa.bloothdevicespeed.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mocxa.bloothdevicespeed.BluetoothConstants;
import com.mocxa.bloothdevicespeed.tools.livedata.LiveDataEvent;

import java.io.IOException;

/**
 * Created by Niraj on 01-01-2022.
 */
public class ConnectThread extends Thread{
    private Boolean mIsSecure;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;


    String mSocketType;
    private final Object myLock = new Object();
    private MutableLiveData<LiveDataEvent<String>> _mEventErrorMessage = new MutableLiveData<LiveDataEvent<String>>();
    public LiveData<LiveDataEvent<String>> mEventErrorMessage = _mEventErrorMessage;
    private MutableLiveData<LiveDataEvent<BluetoothSocket>> _mEventConnected = new MutableLiveData<LiveDataEvent<BluetoothSocket>>();
    public LiveData<LiveDataEvent<BluetoothSocket>> mEventConnected = _mEventConnected;

    public ConnectThread(BluetoothDevice pDevice, Boolean pIsSecure) {
        super();
        this.mDevice = pDevice;
        this.mIsSecure = pIsSecure;
        this.initThread();

    }
    private void initThread() {
        mSocketType = mIsSecure ? "Secure" : "Insecure";
        BluetoothSocket tmp = null;
        try {
            tmp = mIsSecure ?
                    mDevice.createRfcommSocketToServiceRecord(
                            BluetoothConstants.MY_UUID_SECURE
                    )
                    :
                    mDevice.createInsecureRfcommSocketToServiceRecord(
                            BluetoothConstants.MY_UUID_INSECURE
                    );

        } catch (IOException e) {
            Log.e("AcceptThread", "Socket Type: " + mSocketType + "listen() failed", e);
        }
        if (tmp != null) {
            mSocket = tmp;
        }


//        mState = STATE_CONNECTING;
    }

    @Override
    public void run() {
// TODO mAdapter.cancelDiscovery()

        // Make a connection to the BluetoothSocket

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            if (mSocket != null) {
                mSocket.connect();

            }
        } catch (IOException e ) {
            // Close the socket
            e.printStackTrace();
            try {
                if(mSocket!=null){
                    mSocket.close();
                }

            } catch (IOException e2 ) {
                Log.e(
                        "ConnectThread", "unable to close() " + mSocketType +
                                " socket during connection failure", e2
                );
            }
            connectionFailed();
            return;
        }
        if(mSocket!=null){
            _mEventConnected.postValue(new LiveDataEvent<BluetoothSocket>(mSocket));
        }
    }

    private void connectionFailed() {

        _mEventErrorMessage.postValue(new LiveDataEvent<String>("Unable to connect device"));
/*        val msg: Message = mHandlerMain.obtainMessage(BluetoothConstants.CommonEvent.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(BluetoothConstants.CommonEvent.ARG_TOAST_TEXT, "Unable to connect device")
        msg.setData(bundle)
        mHandlerMain.sendMessage(msg)*/
    }

    public void setState(int state ) {

//        TODO   add code here

    }

}
