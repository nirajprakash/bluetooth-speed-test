package com.mocxa.bloothdevicespeed.eeg.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mocxa.bloothdevicespeed.BluetoothConstants;
import com.mocxa.bloothdevicespeed.common.AcceptThread;
import com.mocxa.bloothdevicespeed.common.ConnectThread;
import com.mocxa.bloothdevicespeed.device.DeviceCommands;
import com.mocxa.bloothdevicespeed.eeg.model.ModelPacketEventAck;
import com.mocxa.bloothdevicespeed.eeg.model.ModelPacketEventNack;
import com.mocxa.bloothdevicespeed.eeg.EEGModelPacket;
import com.mocxa.bloothdevicespeed.eeg.EEGPacketHelper;
import com.mocxa.bloothdevicespeed.tools.UtilLogger;
import com.mocxa.bloothdevicespeed.tools.livedata.LiveDataEvent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Niraj on 17-01-2022.
 */
public class BluetoothEEGDeviceController {

    private UtilLogger log = UtilLogger.with(this);
    private BluetoothSocket mSocket = null;
    BluetoothAdapter mAdapter;
    Handler mReadHandler;
    public Timer timer = new Timer();

    private AcceptThread mSecureAcceptThread = null;

    //    private var mInsecureAcceptThread: AcceptThread? = null
    private ConnectThread mConnectThread = null;
    private EEGDeviceSenderThread mDeviceSenderThread = null;
    private EEGDeviceReceiveThread mReceiverService = null;

    private ProcessingThread mProcessingThread = null;

    private int mState = BluetoothConstants.STATE_NONE;

    private boolean mTransmissionPacketSent = false;


    ConcurrentLinkedQueue<byte[]> mPacketQueue = new ConcurrentLinkedQueue<byte[]>();

    ConcurrentLinkedQueue<String> mSetupQueue = new ConcurrentLinkedQueue<String>();

    private MutableLiveData<LiveDataEvent<String>> _mEventErrorMessage = new MutableLiveData<LiveDataEvent<String>>();
    MutableLiveData<LiveDataEvent<String>> mEventErrorMessage = _mEventErrorMessage;
    MediatorLiveData<LiveDataEvent<String>> mEventErrorMessageMediator = new MediatorLiveData<LiveDataEvent<String>>();


    private MutableLiveData<LiveDataEvent<BluetoothSocket>> _mEventConnected = new MutableLiveData<LiveDataEvent<BluetoothSocket>>();
    MutableLiveData<LiveDataEvent<BluetoothSocket>> mEventConnected = _mEventConnected;

    private MutableLiveData<LiveDataEvent<String>> _mEventNackError = new MutableLiveData<LiveDataEvent<String>>();
    MutableLiveData<LiveDataEvent<String>> mEventNackError = _mEventNackError;

    private MutableLiveData<LiveDataEvent<String>> _mEventMessage = new MutableLiveData<LiveDataEvent<String>>();
    MutableLiveData<LiveDataEvent<String>> mEventMessage = _mEventMessage;


    MediatorLiveData<LiveDataEvent<BluetoothSocket>> mEventConnectedMediator = new MediatorLiveData<LiveDataEvent<BluetoothSocket>>();


    private EEGDeviceGate mDeviceGate = new EEGDeviceGate();
    boolean mIsReceiving = true;


    private HandlerThread mSendThread;
    private TimerTask mPeriodicSender;

    private boolean mIsSendingSetup = false;

    private boolean mHoldHeartBeat = false;

    public BluetoothEEGDeviceController(BluetoothAdapter pAdapter, Handler pHandler) {
        mAdapter = pAdapter;
        mReadHandler = pHandler;

    }

    /* *****************************************************************
     *                                      log
     */
    public String logService() {

        long currentTime = System.currentTimeMillis();

        String logMessage = "";
        if (mReceiverService != null) {
            long timeDiff = (currentTime - mReceiverService.getStartTime());
            long byteCounter = mReceiverService.getByteCounter();

            logMessage =" Receiving:  \n" +
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
            log.i(logMessage);

        }

        if(mDeviceGate!=null){
            mDeviceGate.logGate();
        }
        if(TextUtils.isEmpty(logMessage)){
            return "No result";
        }
        return logMessage;


    }

    public void resetCounterLog() {

        if (mReceiverService != null) {
            mReceiverService.resetLog();
        }

        if(mDeviceGate!=null){
            mDeviceGate.resetLog();
        }
    }




    public void stop() {


        clearAccept();
        clearConnect();

        clearReceiver();
        clearSender();

        if (mSendThread != null && !mSendThread.isInterrupted()) {
            mSendThread.interrupt();
        }

        if (mPeriodicSender != null) {
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
        log.i("Socket: "+ mSocket.getMaxReceivePacketSize() +" " + mSocket.getMaxTransmitPacketSize());
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
            mProcessingThread = new ProcessingThread();
            mProcessingThread.start();

            mReceiverService = new EEGDeviceReceiveThread(mSocket, mReadHandler, mDeviceGate);

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

            mDeviceSenderThread = new EEGDeviceSenderThread(mSocket, mDeviceGate, mSendThread);
            if (mState == BluetoothConstants.STATE_CONNECTED) {
                mDeviceSenderThread.toggleConnected(true);
            } else {
                mDeviceSenderThread.toggleConnected(false);

            }
            mDeviceSenderThread.start();
//       TODO     mDeviceSenderThread.setupHandler();
            return true;
        } else {
            return false;
        }
    }

    /* ****************************************************************************
     *                                    Packet Setup
     */
    public void sendSetup() {
        if (mDeviceSenderThread == null || mSendThread == null) return;

        mSetupQueue.clear();

      /*  String message = DeviceCommands.INITIAL_HEART_BEAT;
        mSetupQueue.add(message);

        message = DeviceCommands.HEART_BEAT;
        mSetupQueue.add(message);*/

        String message = DeviceCommands.deciData1();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData2();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData3();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData4();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData5();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData6();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData7();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData8();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData9();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData10();
        mSetupQueue.add(message);

        message = DeviceCommands.deciData11();
        mSetupQueue.add(message);

        message = DeviceCommands.END_ONGOING;
        mSetupQueue.add(message);

        mIsSendingSetup =  true;

        new Handler(mSendThread.getLooper()).postDelayed(() -> {
            if (mDeviceSenderThread != null) {
                String nextMessage = mSetupQueue.peek();
                if (nextMessage != null) {
                    log.i("send Message Setup 1: ");

                    mDeviceGate.incrementWriteCounter(1);
                    mDeviceSenderThread.sendMessage(nextMessage);
                }

            }
        }, 100L);

    }


    private void sendSetupRetryMessage() {
        new Handler(mSendThread.getLooper()).post(() -> {
            if (mDeviceSenderThread != null) {
                String nextMessage = mSetupQueue.peek();
                if (nextMessage != null) {

                    mDeviceGate.incrementWriteCounter(1);
                    mDeviceSenderThread.sendMessage(nextMessage);
                }else{
                    mIsSendingSetup = false;
                    mDeviceGate.toggleSetup(false);
                    _mEventMessage.postValue(new LiveDataEvent<>("Setup Sent"));
                }

            }
        });
    }

    private void sendSetupNextMessage() {
        new Handler(mSendThread.getLooper()).post(() -> {
            if (mDeviceSenderThread != null) {
                mSetupQueue.poll();
                String nextMessage = mSetupQueue.peek();
                if (nextMessage != null) {
                    mDeviceGate.incrementWriteCounter(1);
                    mDeviceSenderThread.sendMessage(nextMessage);
                }else{
                    mIsSendingSetup = false;
                    _mEventMessage.postValue(new LiveDataEvent<>("Setup Sent"));
                }

            }
        });
    }

    public void retrySend() {

        mDeviceGate.setError(false);
        if(mIsSendingSetup){
            sendSetupRetryMessage();
        }

    }






    public int getState() {
        return mState;
    }

    public void stopChat() {
        if (mReceiverService != null) {
            mReceiverService.toggleConnected(false);
//            mReceiverService.interrupt();
        }

        clearReceiver();


        if (mDeviceSenderThread != null) {
            mDeviceSenderThread.toggleConnected(false);
            mDeviceSenderThread.interrupt();
        }

        clearSender();

        if (mPeriodicSender != null) {
            mPeriodicSender.cancel();
            mPeriodicSender = null;
            timer.cancel();
        }


    }



    public void startEEG() {

        mIsSendingSetup = false;

        new Handler(mSendThread.getLooper()).post(() -> {


            mDeviceGate.incrementWriteCounter(3);

            String message = DeviceCommands.INITIAL_HEART_BEAT;
            mDeviceSenderThread.sendMessage(message);
            log.i("startEEG initial:  " + message);


            message = DeviceCommands.HEART_BEAT;
            mDeviceSenderThread.sendMessage(message);
            log.i("startEEG HEART_BEAT:  " + message);

            mDeviceSenderThread.sendMessage(DeviceCommands.IMPEDENCE_OP);
            log.i("startEEG IMPEDENCE_OP:  " + message);




            mPeriodicSender = new TimerTask() {
                @Override
                public void run() {
                    periodicSend();
                }
            };
            timer.schedule(mPeriodicSender, 1000l, 1000 * 1 * 1);

        });


    }

    public void stopEEG() {


        new Handler(mSendThread.getLooper()).post(() -> {
            mDeviceGate.setError(false);
            mDeviceGate.clearAcknowledge();

            mDeviceGate.incrementWriteCounter(1);
            mDeviceSenderThread.sendMessage(DeviceCommands.STOP);

            stopChat();


        });


    }

    private void periodicSend() {
        new Handler(mSendThread.getLooper()).post(() -> {
            if(mHoldHeartBeat){
                return;

            }

            boolean isIncremented = false;
            if (!mTransmissionPacketSent) {
                isIncremented = mDeviceGate.incrementWriteCounter(2);
//                mDeviceGate.incrementWriteCounter(1);
                mDeviceSenderThread.sendMessage(DeviceCommands.TRANSMISSION);

                mTransmissionPacketSent = true;
            }else{
                isIncremented = mDeviceGate.incrementWriteCounter(1);
            }
//            mDeviceGate.incrementWriteCounter(1);
            if(isIncremented){
                mDeviceSenderThread.sendMessage(DeviceCommands.HEART_BEAT);
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


    /* ********************************************************************************
     *                         packet events
     */
    private void onPacketNackEvent(List<ModelPacketEventNack> nackEvents) {

        if(mIsSendingSetup){
            String sentMessage = mSetupQueue.peek();
            if(sentMessage==null){
                sentMessage ="";
            }
            _mEventNackError.postValue(new LiveDataEvent<>("NACK:  \n " +
                    nackEvents.get(0).getStatus() +
                    " \n "+ sentMessage));
        }else{
            mDeviceGate.setError(true);
            _mEventNackError.postValue(new LiveDataEvent<>("NACK:  \n " +
                    nackEvents.get(0).getStatus() +
                    " \n "+ "in other case"));
        }

    }

    private void onPacketAckEvent(List<ModelPacketEventAck> ackEvents) {
        if(mIsSendingSetup){
            sendSetupNextMessage();
        }else{
            mDeviceGate.acknowledge();
        }
    }

    private void onPacketNoAckAndNack() {
        if(mIsSendingSetup){
            mDeviceGate.allowRead(true);
        }
    }

    public void onBytes(byte[] readBuf) {
        mPacketQueue.add(readBuf);
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
        if (mDeviceSenderThread != null) {
            mDeviceSenderThread.interrupt();
        }
//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mDeviceSenderThread = null;

    }

    private void clearReceiver() {
        if (mReceiverService != null) {
            mReceiverService.interrupt();
            mProcessingThread.interrupt();
            mPacketQueue.clear();
        }


//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mReceiverService = null;
        mProcessingThread = null;

    }

    public void holdHeartBeat() {
        mHoldHeartBeat =  true;

    }



    /* *********************************************************************************
     *                          Processing
     */

    private class ProcessingThread extends Thread {

        int mProcessingCounter = 0;
        int mPartialPacketFirstPartLength = 0;
        int mPartialPacketLastPartLength = 0;
        boolean mEEGSpecialPacketStatus = false;
        byte[] eegSpecialPacket;// = new Byte[]


        public ProcessingThread() {
            super();
            eegSpecialPacket = new byte[EEGPacketHelper.getT4PacketSize(DeviceCommands.channel_nos)];
        }


        @Override
        public void run() {
            while (!isInterrupted()) {

                byte[] databuf = mPacketQueue.poll();
                if (databuf != null) {
//                    log.i("Processing  ");
                    mProcessingCounter++;
                    EEGModelPacket packet = EEGPacketHelper.processBuffer(databuf,
                            DeviceCommands.channel_nos,
                            eegSpecialPacket,
                            mEEGSpecialPacketStatus,
                            mPartialPacketFirstPartLength,
                            mPartialPacketLastPartLength);

                    if (packet.ackEvents.size() > 0) {
                        for (ModelPacketEventAck event : packet.ackEvents
                        ) {
                            log.i("Ack event: " + System.currentTimeMillis() + " " + event.getStatus());

                        }

                    }

                    if (packet.nackEvents.size() > 0) {
                        for (ModelPacketEventNack event : packet.nackEvents
                        ) {
                            log.i("Nack event: " + System.currentTimeMillis() + " " + event.getStatus());

                        }
                    }

                    mEEGSpecialPacketStatus = packet.eegSpecialpacketStatus;
                    mPartialPacketFirstPartLength = packet.partialPacketFirstPartLength;
                    mPartialPacketLastPartLength = packet.partialPacketLastPartLength;

                    if (packet.nackEvents.size() > 0) {
                        onPacketNackEvent(packet.nackEvents);
                    } else if (packet.ackEvents.size() > 0) {
                        onPacketAckEvent(packet.ackEvents);
                    }else {
                        onPacketNoAckAndNack();
                    }

                }
//
            }
        }
    }


}
