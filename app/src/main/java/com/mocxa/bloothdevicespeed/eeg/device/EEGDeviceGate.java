package com.mocxa.bloothdevicespeed.eeg.device;

import android.os.Handler;
import android.os.HandlerThread;

import com.mocxa.bloothdevicespeed.tools.UtilLogger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Niraj on 17-01-2022.
 */
public class EEGDeviceGate {

    private UtilLogger log = UtilLogger.with(this);

    public AtomicBoolean mReadActive = new AtomicBoolean(false);
    public AtomicBoolean mWriteActive = new AtomicBoolean(false);
    public AtomicBoolean mAck = new AtomicBoolean(false);
    public AtomicBoolean mNack = new AtomicBoolean(false);

    final Object mMyLock = new Object();

    AtomicInteger mWaitingWriteCounter = new AtomicInteger(-1);

    public AtomicBoolean mIsSetupMode = new AtomicBoolean(true);
    public AtomicBoolean mAllowReadMode = new AtomicBoolean(false);
//    private final long WRITE_MAX_PERIOD = 5L;
//    private final long READ_MIN_PERIOD = 80L;

    //    private final long TIMER_PERIOD = 100L;
    private long mTimerStart = System.currentTimeMillis();
    private long mWriteStartTime = System.currentTimeMillis();

    int mAckCounterLog = 0;
    int mNackCounterLog = 0;
    int mNackCallCounterLog = 0;

    int mReadCounterLog = 0;
    int mWriteCounterLog = 0;
    long mReadPeriodLog = 0;
    long mWritePeriodLog = 0;
//    private boolean mWriteNeeded = false;


    public void holdRead() {

        if (mReadActive.get()) {
            long currentTime = System.currentTimeMillis();
            long periodRead = currentTime - mTimerStart;

//            if (periodRead > READ_MIN_PERIOD) {
            if (mReadActive.compareAndSet(true, false)) {
                mReadPeriodLog += periodRead;
//                log.i("hold Read");
                enableWrite(currentTime);
            }
        }
//            }

    }

    public void holdReadForced() {
        if (mReadActive.get()) {

            if (mIsSetupMode.get()) {
                long currentTime = System.currentTimeMillis();
                long periodRead = currentTime - mTimerStart;
                allowRead(false);
//            if (periodRead > READ_MIN_PERIOD) {
                if (mReadActive.compareAndSet(true, false)) {

                    mReadPeriodLog += periodRead;
//                log.i("hold Read");
                }
            }else{
                holdRead();
            }


        }

    }

    public void enableRead() {

        if (mWriteActive.get()) {
            return;
        }
        if (mWaitingWriteCounter.get() != 0) {
            return;
        }
        if (!shouldRead()) {
            return;
        }

        if (mReadActive.compareAndSet(false, true)) {
            mAck.compareAndSet(true, false);
            mReadCounterLog++;
            log.i("enable Read");

            mTimerStart = System.currentTimeMillis();
        }


    }

    private boolean shouldRead() {
        if (mIsSetupMode.get()) {

            if (!mAllowReadMode.get()) {
                return false;
            }

            if (mNack.get()) {
                return false;
            }

            if (mAck.get()) {
                return false;
            }

        } else {
            return true;
        }
        return true;
    }

    public void acknowledge() {
        synchronized (mMyLock) {
            if (mAck.compareAndSet(false, true)) {
                mAckCounterLog++;
            }
        }

    }

    public void setError(boolean hasError) {
        synchronized (mMyLock) {

            if (mNack.compareAndSet(!hasError, hasError)) {
                if (hasError) {
                    mNackCounterLog++;
                }
            }
            mNackCallCounterLog++;
        }
    }

    public void allowRead(boolean allow) {
        mAllowReadMode.compareAndSet(!allow, allow);
    }

    public void toggleSetup(boolean isSetting){
        mIsSetupMode.compareAndSet(!isSetting, isSetting);
    }


    public void holdWrite() {
        synchronized (mMyLock) {
            if (mWriteActive.get()) {
                long currentTime = System.currentTimeMillis();
                long periodWrite = currentTime - mWriteStartTime;
                if (mWriteActive.compareAndSet(true, false)) {
                    // TODO do some thing here
                    mWritePeriodLog += periodWrite;
//                log.i("hold Write");

                }
            }

        }
    }


    private void holdWriteSilently() {
        if (mWriteActive.compareAndSet(true, false)) {
            long currentTime = System.currentTimeMillis();
            long periodWrite = currentTime - mWriteStartTime;

            mWritePeriodLog += periodWrite;
        }
    }

    public void enableWrite() {


        log.i("enableWrite 1: ");
        enableWrite(System.currentTimeMillis());
    }

    private void enableWrite(long currentTime) {

        if (mReadActive.get()) {
            return;
        }

        if (!shouldWrite()) {
            return;
        }
        if (mWriteActive.compareAndSet(false, true)) {
            mWriteStartTime = currentTime;

            log.i("enableWrite 2: ");
//            log.i("enable Write");
            mWriteCounterLog++;

        }

    }

    private boolean shouldWrite() {
        if (mIsSetupMode.get()) {
            if (mNack.get()) {

                log.i("shouldWrite 1: ");
                return false;
            }

            if (!mAck.get()) {
                log.i("shouldWrite 2: ");
                return false;
            }


        } else {
            return true;
        }

        return true;
    }

    public AtomicBoolean getReadActive() {
        return mReadActive;
    }

    public AtomicBoolean getWriteActive() {
        return mWriteActive;
    }

    public Object getMyLock() {
        return mMyLock;
    }

    public AtomicBoolean getAck() {
        return mAck;
    }

    public AtomicBoolean getNack() {
        return mNack;
    }


    /* ******************************************************************************
     *                                   log
     */


    public void resetLog() {
        synchronized (mMyLock) {
            mAckCounterLog = 0;
            mNackCounterLog = 0;
            mNackCallCounterLog = 0;

            mReadCounterLog = 0;
            mWriteCounterLog = 0;
            mReadPeriodLog = 0;
            mWritePeriodLog = 0;
        }
    }

    public void logGate() {
        log.i("Ack: " + mAckCounterLog + " Nack: " + mNackCounterLog + " Nack Call: " + mNackCallCounterLog);
        log.i(" Read: " + mReadCounterLog + ", period: " + mReadPeriodLog + " Write: " + mWriteCounterLog + ", period: " + mWritePeriodLog);
    }


    public void clearAcknowledge() {
        mAck.compareAndSet(true, false);
    }


    /* *********************************************************************************
     *                                        write counter
     */
    public void decrementWriteCounter(HandlerThread thread) {


        if (mWaitingWriteCounter.get() == 1) {
            new Handler(thread.getLooper()).postDelayed(() -> {

                if (mWaitingWriteCounter.compareAndSet(1, 0)) {
                    if(mIsSetupMode.get()){
                        allowRead(true);
                    }
                    log.i("decrementWriteCounter end: " + 0);
                } else {
                    log.i("decrementWriteCounter end: ");

                }


            }, 100L);
        } else if (mWaitingWriteCounter.get() > 1) {

            int value = mWaitingWriteCounter.decrementAndGet();
            log.i("decrementWriteCounter other: " + value);
        }


    }

    public boolean incrementWriteCounter(int count) {

        if (mWaitingWriteCounter.get() == -1) {

            mWaitingWriteCounter.addAndGet(count + 1);
            log.i("incrementWriteCounter 1:  " + mWaitingWriteCounter.get());

        } else if (mWaitingWriteCounter.get() == 0) {
            mWaitingWriteCounter.addAndGet(count);
            log.i("incrementWriteCounter 2:  " + mWaitingWriteCounter.get());

        } else {
            return false;
        }


        return true;

    }


}
