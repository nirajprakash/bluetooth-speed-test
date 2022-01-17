package com.mocxa.bloothdevicespeed.eeg;

import com.mocxa.bloothdevicespeed.eeg.model.ModelPacketEventAck;
import com.mocxa.bloothdevicespeed.eeg.model.ModelPacketEventNack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niraj on 11-01-2022.
 */
public class EEGModelPacket {

    public int partialPacketFirstPartLength= 0;
    public boolean eegSpecialpacketStatus = false;
    public List<String> patientEvents = new ArrayList<>();
    public List<ModelPacketEventAck> ackEvents = new ArrayList<>();
    public List<ModelPacketEventNack> nackEvents = new ArrayList<>();

    public long processingTimePeriod =0;

    public int partialPacketLastPartLength = 0;

    public List<int[]> eegGraphPacketList=  new ArrayList<>();


}
