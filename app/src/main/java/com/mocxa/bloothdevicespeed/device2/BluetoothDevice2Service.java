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

import java.util.List;
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


    private Device2Gate mDevice2Gate = new Device2Gate();
    boolean mIsReceiving = true;


    private HandlerThread mSendThread;
    private TimerTask mPeriodicSender;

    private boolean mIsSendingSetup = false;

    private boolean mHoldHeartBeat = false;

    public BluetoothDevice2Service(BluetoothAdapter pAdapter, Handler pHandler) {
        mAdapter = pAdapter;
        mReadHandler = pHandler;

    }

    /* *****************************************************************
     *                                      log
     */
    public String logService() {

        long currentTime = System.currentTimeMillis();

        if (mReceiverService != null) {
            long timeDiff = (currentTime - mReceiverService.getStartTime());
            long byteCounter = mReceiverService.getByteCounter();
            log.i(" Receiving:  \n" +
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
                    " \n");

        }

        if(mDevice2Gate!=null){
            mDevice2Gate.logGate();
        }
        return "No result";

    }

    public void resetCounterLog() {

        if (mReceiverService != null) {
            mReceiverService.resetLog();
        }

        if(mDevice2Gate!=null){
            mDevice2Gate.resetLog();
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

    /* ****************************************************************************
     *                                    Packet Setup
     */
    public void sendSetup() {
        if (mDevice2SenderThread == null || mSendThread == null) return;

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
            if (mDevice2SenderThread != null) {
                String nextMessage = mSetupQueue.peek();
                if (nextMessage != null) {
                    log.i("send Message Setup 1: ");

                    mDevice2Gate.incrementWriteCounter(1);
                    mDevice2SenderThread.sendMessage(nextMessage);
                }

            }
        }, 100L);



        /*
        new Handler(mSendThread.getLooper()).postDelayed(() -> {

            TODO
            String message = DeviceCommands.INITIAL_HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup :  " + message);

            message = DeviceCommands.HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup :  " + message);

            message = DeviceCommands.deciData1();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci1:  " + message);


            message = DeviceCommands.deciData2();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci2:  " + message);

            message = DeviceCommands.deciData3();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci3:  " + message);

            message = DeviceCommands.deciData4();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci4:  " + message);

            message = DeviceCommands.deciData5();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci5:  " + message);

            message = DeviceCommands.deciData6();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci6:  " + message);


            message = DeviceCommands.deciData7();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci7:  " + message);

            message = DeviceCommands.deciData8();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci8:  " + message);

            message = DeviceCommands.deciData9();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci9:  " + message);

            message = DeviceCommands.deciData10();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci10:  " + message);

            message = DeviceCommands.deciData11();
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup deci11:  " + message);


            message = DeviceCommands.END_ONGOING;
            mDevice2SenderThread.sendMessage(message);
            log.i("SendSetup end:  " + message);

        }, 100);
        */


    }


    private void sendSetupRetryMessage() {
        new Handler(mSendThread.getLooper()).post(() -> {
            if (mDevice2SenderThread != null) {
                String nextMessage = mSetupQueue.peek();
                if (nextMessage != null) {

                    mDevice2Gate.incrementWriteCounter(1);
                    mDevice2SenderThread.sendMessage(nextMessage);
                }else{
                    mIsSendingSetup = false;
                    _mEventMessage.postValue(new LiveDataEvent<>("Setup Sent"));
                }

            }
        });
    }

    private void sendSetupNextMessage() {
        new Handler(mSendThread.getLooper()).post(() -> {
            if (mDevice2SenderThread != null) {
                mSetupQueue.poll();
                String nextMessage = mSetupQueue.peek();
                if (nextMessage != null) {
                    mDevice2Gate.incrementWriteCounter(1);
                    mDevice2SenderThread.sendMessage(nextMessage);
                }else{
                    mIsSendingSetup = false;
                    _mEventMessage.postValue(new LiveDataEvent<>("Setup Sent"));
                }

            }
        });
    }

    public void retrySend() {

        mDevice2Gate.setError(false);
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


        if (mDevice2SenderThread != null) {
            mDevice2SenderThread.toggleConnected(false);
            mDevice2SenderThread.interrupt();
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


            mDevice2Gate.incrementWriteCounter(3);

            String message = DeviceCommands.INITIAL_HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("startEEG initial:  " + message);


            message = DeviceCommands.HEART_BEAT;
            mDevice2SenderThread.sendMessage(message);
            log.i("startEEG HEART_BEAT:  " + message);

            mDevice2SenderThread.sendMessage(DeviceCommands.IMPEDENCE_OP);
            log.i("startEEG IMPEDENCE_OP:  " + message);




            mPeriodicSender = new TimerTask() {
                @Override
                public void run() {
                    periodicSend();
                }
            };
            timer.schedule(mPeriodicSender, 0l, 1000 * 1 * 1);

        });


    }

    public void stopEEG() {


        new Handler(mSendThread.getLooper()).post(() -> {
            mDevice2Gate.setError(false);
            mDevice2Gate.clearAcknowledge();

            mDevice2Gate.incrementWriteCounter(1);
            mDevice2SenderThread.sendMessage(DeviceCommands.STOP);

            stopChat();


        });


    }

    private void periodicSend() {
        new Handler(mSendThread.getLooper()).post(() -> {
            if(mHoldHeartBeat){
                return;
            }
//            mDevice2Gate.incrementWriteCounter(1);
            mDevice2SenderThread.sendMessage(DeviceCommands.HEART_BEAT);

            if (!mTransmissionPacketSent) {
//                mDevice2Gate.incrementWriteCounter(1);
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
            mDevice2Gate.setError(true);
            _mEventNackError.postValue(new LiveDataEvent<>("NACK:  \n " +
                    nackEvents.get(0).getStatus() +
                    " \n "+ "in other case"));
        }

    }

    private void onPacketAckEvent(List<ModelPacketEventAck> ackEvents) {
        if(mIsSendingSetup){
            sendSetupNextMessage();
        }else{
            mDevice2Gate.acknowledge();
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
        if (mDevice2SenderThread != null) {
            mDevice2SenderThread.interrupt();
        }
//        mReceiverService?.let { mEventErrorMessage.removeSource(it.mEventErrorMessage) }
        mDevice2SenderThread = null;

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
            eegSpecialPacket = new byte[PacketHelper.getT4PacketSize(DeviceCommands.channel_nos)];
        }


        @Override
        public void run() {
            while (!isInterrupted()) {

                byte[] databuf = mPacketQueue.poll();
                if (databuf != null) {
//                    log.i("Processing  ");
                    mProcessingCounter++;
                    ModelPacket packet = PacketHelper.processBuffer(databuf,
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
                    }

                }
//
            }
        }
    }


}
