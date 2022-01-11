package com.mocxa.bloothdevicespeed.device2;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niraj on 11-01-2022.
 */
public class ModelPacket {

    public int partialPacketFirstPartLength= 0;
    public boolean eegSpecialpacketStatus = false;
    public List<String> patientEvents = new ArrayList<>();
    public List<ModelPacketEventAck> ackEvents = new ArrayList<>();
    public List<ModelPacketEventNack> nackEvents = new ArrayList<>();

    long processingTimePeriod =0;

    int partialPacketLastPartLength = 0;

    List<int[]> eegGraphPacketList=  new ArrayList<>();


}
