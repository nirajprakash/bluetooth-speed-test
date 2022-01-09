package com.mocxa.bloothdevicespeed.device2;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Niraj on 09-01-2022.
 */
public class Device2Gate {

    public AtomicBoolean mReadActive = new AtomicBoolean(false);
    public AtomicBoolean mWriteActive = new AtomicBoolean(false);
    public AtomicBoolean mAck = new AtomicBoolean(false);

    public AtomicBoolean mNack = new AtomicBoolean(false);

    final Object mMyLock = new Object();

    private final long WRITE_MAX_PERIOD = 10L;
    private final long READ_MIN_PERIOD = 50L;

    private final long TIMER_PERIOD = 100L;
    private long mTimerStart = System.currentTimeMillis();
    private long mWriteStartTime = System.currentTimeMillis();


    public void holdRead() {
        synchronized (mMyLock) {
            long currentTime = System.currentTimeMillis();
            long periodRead = currentTime - mTimerStart;

            if (periodRead > READ_MIN_PERIOD) {
                if (mReadActive.compareAndSet(true, false)) {
                    enableWrite(currentTime);
                }
            }
        }
    }

    public void enableRead() {
        synchronized (mMyLock) {
            long currentTime = System.currentTimeMillis();
            long timerPeriod = currentTime - mTimerStart;
            if (timerPeriod >= TIMER_PERIOD) {
                if (!mReadActive.get()) {
                    holdWriteSilently();
                    if (mReadActive.compareAndSet(false, true)) {
                        mAck.compareAndSet(true, false);
                        mTimerStart = System.currentTimeMillis();
                    }
                }

            }

        }
    }

    public void acknowledge(){
        synchronized (mMyLock){
            mAck.compareAndSet(false, true);
        }

    }

    public void setError(boolean hasError){
        synchronized (mMyLock) {
            mNack.compareAndSet(!hasError, hasError);
        }
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

    public void holdWrite() {
        synchronized (mMyLock) {
            long currentTime = System.currentTimeMillis();
            long periodWrite = currentTime - mWriteStartTime;
            if (periodWrite > WRITE_MAX_PERIOD) {
                if (mWriteActive.compareAndSet(true, false)) {
                    // TODO do some thing here
                }
            }
        }
    }


    private void holdWriteSilently() {
        mWriteActive.compareAndSet(true, false);
    }

    private void enableWrite(long currentTime) {
        if (mWriteActive.compareAndSet(false, true)) {
            mWriteStartTime = currentTime;
        }
    }


}
