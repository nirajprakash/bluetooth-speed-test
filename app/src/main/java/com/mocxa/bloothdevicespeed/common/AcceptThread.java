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
import java.util.concurrent.locks.Lock;

/**
 * Created by Niraj on 01-01-2022.
 */
public class AcceptThread extends Thread {

    // Unique UUID for this application
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
    private final BluetoothAdapter mAdapter;
    private final Boolean mIsSecure;

    String mSocketType;
    private final Object myLock = new Object();

    public int mState = BluetoothConstants.STATE_NONE;

    BluetoothServerSocket mServerSocket = null;
    private MutableLiveData<LiveDataEvent<String>> _mEventErrorMessage = new MutableLiveData<LiveDataEvent<String>>();
    public LiveData<LiveDataEvent<String>> mEventErrorMessage = _mEventErrorMessage;
    private MutableLiveData<LiveDataEvent<BluetoothSocket>> _mEventConnected = new MutableLiveData<LiveDataEvent<BluetoothSocket>>();
    public LiveData<LiveDataEvent<BluetoothSocket>> mEventConnected = _mEventConnected;


    public AcceptThread(BluetoothAdapter pAdapter, Boolean pIsSecure) {
        super();
        this.mAdapter = pAdapter;
        this.mIsSecure = pIsSecure;
        this.initThread();


    }

    private void initThread() {
        mSocketType = mIsSecure ? "Secure" : "Insecure";
        BluetoothServerSocket tmp = null;
        try {
            tmp = mIsSecure ?
                    mAdapter.listenUsingRfcommWithServiceRecord(
                            NAME_SECURE,
                            BluetoothConstants.MY_UUID_SECURE
                    )
                    :
                    mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, BluetoothConstants.MY_UUID_INSECURE
                    );

        } catch (IOException e) {
            Log.e("AcceptThread", "Socket Type: " + mSocketType + "listen() failed", e);
        }
        if (tmp != null) {
            mServerSocket = tmp;
        }
        mState = BluetoothConstants.STATE_LISTEN;


//        mState = STATE_CONNECTING;
    }

    @Override
    public void run() {
        Log.d(
                "AcceptThread", "Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this
        );
        setName("AcceptThread" + mSocketType);

        BluetoothSocket socket = null;

        // Listen to the server socket if we're not connected

        // Listen to the server socket if we're not connected
        while (!interrupted() && mState != BluetoothConstants.STATE_CONNECTED) {
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                if (mServerSocket != null) {
                    socket = mServerSocket.accept();

                }
            } catch (IOException e) {
                Log.e("AcceptThread", "Socket Type: " + mSocketType + "accept() failed", e);
                break;
            }

            // If a connection was accepted
            if (socket != null) {
                synchronized (myLock) {
                    if (mState == BluetoothConstants.STATE_LISTEN || mState == BluetoothConstants.STATE_CONNECTING) {
                        connected(
                                socket, socket.getRemoteDevice(),
                                mSocketType
                        );
                    } else if (mState == BluetoothConstants.STATE_NONE || mState == BluetoothConstants.STATE_CONNECTED) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e("AcceptThread", "Could not close unwanted socket", e);
                        }
                    }


                }
            }
            Log.i("AcceptThread", "END mAcceptThread, socket Type: $mSocketType");
        }
    }

    private void connected(BluetoothSocket socket, BluetoothDevice remoteDevice, String
            socketType) {
        _mEventConnected.postValue(new LiveDataEvent(socket));

    }
}
