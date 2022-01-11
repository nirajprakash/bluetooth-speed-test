package com.mocxa.bloothdevicespeed.device2;

/**
 * Created by Niraj on 11-01-2022.
 */
public class ModelPacketEventNack {
    byte status1;
    byte status2;

    public ModelPacketEventNack(byte pStatus1, byte pStatus2){
        status1 = pStatus1;
        status2 = pStatus2;
    }
    public String getStatus() {
        return String.format("%02x", status1) + " "+ String.format("%02x", status2);
    }
}
