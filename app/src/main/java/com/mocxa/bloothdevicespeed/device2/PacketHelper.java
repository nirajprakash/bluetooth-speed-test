package com.mocxa.bloothdevicespeed.device2;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Niraj on 11-01-2022.
 */
public class PacketHelper {

    public static final byte[] eegZeroPacket = {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20};

    static final int T4_24_CHANNEL_PACKET_SIZE = 78;
    static final int T4_16_CHANNEL_PACKET_SIZE = 54;
    static final int T4_8_CHANNEL_PACKET_SIZE = 30;
    static final int T4_HEARTBEAT_PACKET_SIZE = 6;

    private static final byte T4_RES_TYPE = (byte) 0x65;


    public static int getT4PacketSize(int channel_nos) {
        int T4_packet_length = 0;
        if (channel_nos == 24) {
            T4_packet_length = T4_24_CHANNEL_PACKET_SIZE;
//            Channel_No_Packet = 0x38;
//            multiplicationFactor=135;
        } else if (channel_nos == 16) {
            T4_packet_length = T4_16_CHANNEL_PACKET_SIZE;
//            Channel_No_Packet = 0x30;
//            multiplicationFactor=100;
        } else if (channel_nos == 8) {
            T4_packet_length = T4_8_CHANNEL_PACKET_SIZE;
//            Channel_No_Packet = 0x28;
//            multiplicationFactor=80;
        }
        return T4_packet_length;
    }


    public static int[] Decrypt_EEG(byte[] dat, int offset, int length, int channel_nos) {

        int T4_packet_length = getT4PacketSize(channel_nos);
        /*
                The data stream that you are receiving is a single sample for all channels.
                The 1st byte = 1c (SOP)
                The 2nd byte = 0x70 = 'p' (packet type)
                The 3rd byte = 0x21 (less 0x20 is start channel = 1)
                The 4th byte = 0x64 (less 0x20 is number of channels = 68)
                The 5th byte = 0x65 (less 0x20 is Res/Type = 0x45 (Type=5, Res=8))
                Each channel is then represented by 3 bytes to encode the 24 bits acquired data.
                The order is Q1, Q2 and R according to the Interface Spec:
                Encode (b y T4): For the 23-bit resolution, the value is shifted right by X
                and then divided by Y1 giving Q1 and R1. Then R1 is divided by Y2 giving Q2 and R.
                These three values have 0x20 added to them before transmission.
                Decode (by PC): For the 23-bit resolution, 0x20 is subtracted first from all values, then Q1 is multiplied by Y1. This is
                added to Q2 multiplied by Y2. This is added to R and the sum shifted left by X.
                This means ((Q1 * Y1) + (Q2 * Y2) + R) << X
                Make sure that you end up with a signed 24bit number which you can sign extend into a 32 bit quantity.
 */
        int count, sop, eop, packettypepos, stchannel, nochannels, parsednochannels, restype, parsedrestype, Q1_pos, Q2_pos, R_pos;
        byte Q1_value, Q2_value, R_value;
        int[] chn_no;
        int[] eegdata_decrypted = new int[channel_nos];
        //int uQ1,uQ2,uR;

        int x = 1;
        int y1 = 40960;
        int y2 = 192;

        // Log.d("Mocxa-Decrypt EEG Method", "Initalizations done");
        byte[] eeg_dat;
        int eeg_dat_offset;
        int packetSize = (dat[offset + 3] - (byte) 0x20) * 3;

        if (length < packetSize) {
            eeg_dat = new byte[T4_packet_length];
            System.arraycopy(dat, offset, eeg_dat, 0, length);

            int destlength = packetSize - length;
            if ((packetSize - length) >= eeg_dat.length) {
                destlength = eeg_dat.length;
            }
            System.arraycopy(eegZeroPacket, 0, eeg_dat, length, destlength);
            eeg_dat_offset = 0;
        } else {
            eeg_dat = dat;
            eeg_dat_offset = offset;
        }
        //Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt EEG Method: data  length= " + eeg_dat.length);
        // Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt EEG Method: data  = " + Arrays.toString(eeg_dat));
        // Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt EEG Method: Offset= " + eeg_dat_offset);
        //  Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt EEG Method: Length= " + length);
        int startChannel = eeg_dat[eeg_dat_offset + 2] - (byte) 0x20;
        int numOfChannels = eeg_dat[eeg_dat_offset + 3] - (byte) 0x20;

        //Log.d("Moxca", "In if statement in Decrypt method startChannel from packet(dat[offset + 2]: " + startChannel);
        // Log.d("Moxca", "In if statement in Decrypt method No of channels from packet(dat[offset + 4]: " + numOfChannels);
        try {

            //   Log.d("Mocxa-Decrypt EEG Method", "Inside -try loop " + eeg_dat_offset);
            /*TODO commented packetcount++;
            packetCountsem.acquire();
            packetCount_Global++;
            packetCountsem.release();*/
            //  Log.d("Mocxa-Decrypt EEG Method", "Packet count incremented packetcount: " + packetcount);
            for (int idx = 1; idx <= channel_nos; idx++) {
                // Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt Efor loop idx:: = " + idx);
                // Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt Efor loop startChannel:: = " + startChannel);
                // Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt Efor loop channel_nos:: = " + channel_nos);

                if (idx < startChannel) {

                    Q1_value = (byte) 0x0;
                    Q2_value = (byte) 0x0;
                    R_value = (byte) 0x0;

                    int uQ1 = (int) Q1_value & 0xff;
                    int uQ2 = (int) Q2_value & 0xff;
                    int uR = (int) R_value & 0xff;


                    eegdata_decrypted[idx - 1] = (((((uQ1 - (byte) 0x20) * y1) + ((uQ2 - (byte) 0x20) * y2) + (uR - (byte) 0x20)) << x) << 8) / 256;


                    eeg_dat_offset += 3;
                    // Log.d("Mocxa-Decrypt EEG Method", "EEG Data for CH- idx < startChannel " + idx + "::" + eegdata_decrypted[idx-1]);
                    //  Log.d("Mocxa-Decrypt EEG Method", "Q1:  idx < startChannel " + uQ1);
                    //  Log.d("Mocxa-Decrypt EEG Method", "Q2:  idx < startChannel " +  "::" + uQ2);
                    //   Log.d("Mocxa-Decrypt EEG Method", "R:  idx < startChannel " + "::" + uR);


                } else if (idx >= startChannel && idx < (startChannel + numOfChannels)) {
                    Q1_value = (byte) (eeg_dat[eeg_dat_offset + 5]);
                    Q2_value = (byte) (eeg_dat[eeg_dat_offset + 6]);
                    R_value = (byte) (eeg_dat[eeg_dat_offset + 7]);

                    int uQ1 = (int) Q1_value & 0xff;
                    int uQ2 = (int) Q2_value & 0xff;
                    int uR = (int) R_value & 0xff;


                    eegdata_decrypted[idx - 1] = (((((uQ1 - (byte) 0x20) * y1) + ((uQ2 - (byte) 0x20) * y2) + (uR - (byte) 0x20)) << x) << 8) / 256;

                    eeg_dat_offset += 3;
                    // Log.d("Mocxa-Decrypt EEG Method", "EEG Data for CH-" + idx + "::" + eegdata_decrypted[idx-1]);
                    //  Log.d("Mocxa-Decrypt EEG Method", "EEG Data for CH-" + idx + "::" + eegdata_decrypted[idx-1]);
                    // Log.d("Mocxa-Decrypt EEG Method", "Q1: " + "else if (idx >= startChannel" + "::" + uQ1);
                    //   Log.d("Mocxa-Decrypt EEG Method", "Q2: " + "else if (idx >= startChannel" + "::" + uQ2);
                    // Log.d("Mocxa-Decrypt EEG Method", "R: " + "else if (idx >= startChannel" + "::" + uR);


                } else if (idx >= (startChannel + numOfChannels) && (idx < channel_nos)) {

                    Q1_value = (byte) 0x0;
                    Q2_value = (byte) 0x0;
                    R_value = (byte) 0x0;

                    int uQ1 = (int) Q1_value & 0xff;
                    int uQ2 = (int) Q2_value & 0xff;
                    int uR = (int) R_value & 0xff;


                    eegdata_decrypted[idx - 1] = (((((uQ1 - (byte) 0x20) * y1) + ((uQ2 - (byte) 0x20) * y2) + (uR - (byte) 0x20)) << x) << 8) / 256;


                    eeg_dat_offset += 3;
                    //  Log.d("Mocxa-Decrypt EEG Method", "EEG Data for CH-" + idx+ "::" + eegdata_decrypted[idx-1]);
                    //  Log.d("Mocxa-Decrypt EEG Method", "EEG Data for CH-" + idx + "::" + eegdata_decrypted[idx-1]);
                    //  Log.d("Mocxa-Decrypt EEG Method", "Q1: " + "else if (idx >= (startChannel + numOfChannels) && (idx < channel_nos)" + "::" + uQ1);
                    //  Log.d("Mocxa-Decrypt EEG Method", "Q2: " + "idx >= (startChannel + numOfChannels) && (idx < channel_nos)" + "::" + uQ2);
                    //   Log.d("Mocxa-Decrypt EEG Method", "R: " + "idx >= (startChannel + numOfChannels) && (idx < channel_nos)" + "::" + uR);

                }
 /* ToDo remove in future
                if (pipactive == false) {
                    EEG_Data_Display[idx - 1] = (float) butterworth.filter(eegdata_decrypted[idx - 1] * gain);
                    EEG_Data_Display_list[idx - 1][getDatacountAccumalated] = EEG_Data_Display[idx - 1];
                    //Log.d("Mocxa-Decrypt EEG Method", "Inside --Decrypt EEG Method: EEG_Data_Display_list[idx - 1][getDatacountAccumalated] = "+ ( idx - 1) + " " + getDatacountAccumalated+" "  +  EEG_Data_Display_list[idx - 1][getDatacountAccumalated]);
                  //  Log.d("Mocxa-Decrypt EEG Method after butterworth calculation", "Inside --Decrypt EEG Method: EEG_Data_Display_list[idx - 1][getDatacountAccumalated] = "+ ( idx - 1) + " " + getDatacountAccumalated+" "  +  EEG_Data_Display_list[idx - 1][getDatacountAccumalated]);
                } */

               /*

               TODO Not using for now related to impedance
               if (impedenceCheck == true) {
                    // Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: " + impedenceCheck);

                    if (packetcount == 1) {
                        // Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: packetcount == 1 packetcount: " + packetcount);
                        min_val_ch[idx - 1] = eegdata_decrypted[idx - 1];
                        max_val_ch[idx - 1] = eegdata_decrypted[idx - 1];
                        //  Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: min_val_ch[idx - 1]  "+ (idx - 1)+" : " + min_val_ch[idx - 1]);
                        // Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true:  max_val_ch[idx - 1]  "+ (idx - 1)+" : " +  max_val_ch[idx - 1]);
                    } else if (packetcount <= 125) {
                        // Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: packetcount <= 125 packetcount: " + packetcount);
                        if (eegdata_decrypted[idx - 1] < min_val_ch[idx - 1]) {
                            min_val_ch[idx - 1] = eegdata_decrypted[idx - 1];
                            // Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: min_val_ch[idx - 1]  "+ (idx - 1)+" : " + min_val_ch[idx - 1]);
                        }
                        if (eegdata_decrypted[idx - 1] > max_val_ch[idx - 1]) {
                            max_val_ch[idx - 1] = eegdata_decrypted[idx - 1];
                            // Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true:  max_val_ch[idx - 1]  "+ (idx - 1)+" : " +  max_val_ch[idx - 1]);
                        }
                        *//* ToDo remove in futre
                        if (packetcount == 125) {
                            //  Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: packetcount == 125 packetcount: " + packetcount);
                            int peakToPeakVal = max_val_ch[idx - 1] - min_val_ch[idx - 1];
                            Impedance_ch[idx - 1] = (peakToPeakVal * 0.9f - 5000);
                            //   Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true:  peakToPeakVal : " +  peakToPeakVal);
                            //   Log.d("Moxca", "In Decrypt method Inside impedenceCheck == true: Impedance_ch[idx - 1]  "+ (idx - 1)+" : " + Impedance_ch[idx - 1]);
                        }*//*
                    }
                }*/

            }
        } catch (Exception e) {
            e.printStackTrace();
            //  Log.d("Moxca", "In catch statement in Decrypt method" + e);

        }

        //  Log.d("Moxca", " in Decrypt method packet number: " + packetcount);
        /* TODO remove in future
        if (packetcount == 125) {
            packetcount = 0;
            //  Log.d("Moxca", " in Decrypt method packet count set to zero after packet count reached 125; packetcount : " + packetcount);
        }*/

       /*
        TODO uncomment
        getDatacountAccumalated++;
        if (getDatacountAccumalated == datacountAccumalate) {

            getDatacountAccumalated = 0;


            if (pipactive == false) {
                if (channel_nos == 24) {

                    //addEntry_graph_frg_24(EEG_Data_Display_list);

                }

//                TODO remove this EEG_Data_Display_list = new double[channel_nos][datacountAccumalate];
            }

        }
        //  Log.d("Moxca", " in Decrypt method packet number: " + packetCount_Global);
        if (pipactive) {
            plotData = false;
            Startplot = true;
            //TODO uncomment fileWriteHandler.fileupdate(eegdata_decrypted);
           *//* ToDo remove in future
            fileupdate(eegdata_decrypted); *//*
        }*/
        return eegdata_decrypted;
    }

    // Note that you need to pass offset - 2 here
    private static int GetEOPOffset(byte[] readBuffer, int offset) {
        // Log.d("Mocxa", "Inside GetEOPOffset ; offset: " +offset);
        int newOffset = offset;
        boolean offsetFound = false;
        do {
            if (offset < 0) {
                // Log.d("Mocxa", "Inside GetEOPOffset ; offset < 0  returns -1 offset: " +offset);
                return -1;
            }
            if (readBuffer[offset] == (byte) 0x0D) {
                // Log.d("Mocxa", "Inside GetEOPOffset ; readBuffer[offset] == (byte)0x0D offset: " +offset);
                newOffset = offset;
                offsetFound = true;
                //Log.d("Mocxa", "Inside GetEOPOffset ; readBuffer[offset] == (byte)0x0D newOffset: " +newOffset);
                //Log.d("Mocxa", "Inside GetEOPOffset ; readBuffer[offset] == (byte)0x0D offsetFound: " +offsetFound);

            } else {
                --offset;
                //Log.d("Mocxa", "Inside GetEOPOffset ; else part of readBuffer[offset] == (byte)0x0D --offset; offset: " +offset);
            }
        } while (offsetFound == false);
        //Log.d("Mocxa", "Inside GetEOPOffset ;returns new offset   newOffset+1 " +newOffset+1);
        return newOffset + 1;
    }


    // Pass offset + 1
    private static int GetSOPOffset(byte[] readBuffer, int offset, int bufferLength) {
        int newOffset = offset;
        boolean offsetFound = false;
        //  Log.d("Mocxa", "Inside GetSOPOffset ; offset: " +offset);
        do {
            if (offset == bufferLength) {
                // Log.d("Mocxa", "Inside GetSOPOffset ; inside offset == bufferLength offset: " +offset);
                return -1;
            }
            if (readBuffer[offset] == (byte) 0x1C) {
                newOffset = offset;
                offsetFound = true;
                // Log.d("Mocxa", "Inside GetSOPOffset ; inside readBuffer[offset] == (byte)0x1C  offset: " +offset);
                //  Log.d("Mocxa", "Inside GetSOPOffset ; inside readBuffer[offset] == (byte)0x1C  newOffset: " +newOffset);
                //  Log.d("Mocxa", "Inside GetSOPOffset ; inside readBuffer[offset] == (byte)0x1C  offsetFound: " +offsetFound);

            } else {
                ++offset;
                // Log.d("Mocxa", "Inside GetSOPOffset ; inside else part of readBuffer[offset] == (byte)0x1C  offset++: " +offset);
            }
        } while (offsetFound == false);
        //Log.d("Mocxa", "Inside GetSOPOffset ; return value of new offset   newOffset: " +newOffset);
        return newOffset;
    }

    //check the validity of packet based on packet header

    private static boolean checkPacketMetadata(byte[] readBuffer, int offset, int partialPacketFirstPartLength, int bufferLength) {
        boolean validPacket = true;
        byte packetType = (byte) 0x0;
        byte numChannels = (byte) 0x0;

        switch (partialPacketFirstPartLength) {
            case 1:
                if (offset + 2 < bufferLength) {
                    packetType = readBuffer[offset];
                    numChannels = readBuffer[offset + 2];
                }
                break;
            case 2:
                if (offset + 1 < bufferLength) {
                    numChannels = readBuffer[offset + 1];
                }
                break;
            case 3:
                if (offset < bufferLength) {
                    numChannels = readBuffer[offset];
                }
                break;
        }

        if (packetType != (byte) 0x70 || packetType != (byte) 0x68 || packetType != (byte) 0x61 || packetType != (byte) 0x6E) {
            validPacket = false;
        }

        if (numChannels != (byte) 0x38 || numChannels != (byte) 0x28 || numChannels != (byte) 0x30) {
            validPacket = false;

        }

        return validPacket;
    }

    public static ModelPacket processBuffer(byte[] databuf, int channel_nos, byte[] eegSpecialPacket,
                                            boolean prevEegSpecialpacketStatus,
                                            int prevPartialPacketFirstPartLength,
                                            int prevPartialPacketLastPartLength) {

        ModelPacket modelPacket = new ModelPacket();
        List<int[]> eegGraphPacketList = new ArrayList<>();
        modelPacket.eegGraphPacketList = eegGraphPacketList;

        int partialPacketFirstPartLength = prevPartialPacketFirstPartLength;
        modelPacket.partialPacketFirstPartLength = prevPartialPacketFirstPartLength;

        int partialPacketLastPartLength = prevPartialPacketLastPartLength;
        modelPacket.partialPacketLastPartLength = prevPartialPacketLastPartLength;

        boolean eegSpecialpacketStatus = prevEegSpecialpacketStatus;
        modelPacket.eegSpecialpacketStatus = prevEegSpecialpacketStatus;

        List<String> patientEvents = new ArrayList<>();
        modelPacket.patientEvents = patientEvents;

        List<ModelPacketEventAck> ackEvents = new ArrayList<>();
        modelPacket.ackEvents = ackEvents;

        List<ModelPacketEventNack> nackEvents = new ArrayList<>();
        modelPacket.nackEvents = nackEvents;

        int packetSize = 0;

        if (databuf == null) return modelPacket;


        Long startTimeNano = System.nanoTime();
        // TODO outside processingCounter++;

        int T4_packet_length = getT4PacketSize(channel_nos);
                    /*try {
                        packetSemaphore.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
        // byte[] databuf = packetQueue.remove();
        //packetSemaphore.release();
        //totalbytesread += databuf.length ;
        //Log.d("Mocxa", "data buffer = " +totalbytesread);
        // databuf = (byte[]) packetQueueIterator.next();
        // System.out.println("Queue Next Value :" + databuf);
        int bufferLength = databuf.length;
        // int bufferLength = (bufferLengthQueue.remove()).intValue();
        //
        // case 1- 1c and 0d
        boolean endOfPacket = false;
        if (databuf[0] == (byte) 0x1c && databuf[bufferLength - 1] == (byte) 0x0d) {
            // In this case, you have multiple full packets
            // Also it might have the heartbeat packets (6 bytes)
            // In addition to checking for 0x1C for each packet, we
            // need to check for the byte next to 1C. If it is 0x70
            // then it is an EEG data packet. Otherwise, it is a
            // heartbeat response
            //Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d ");

            // check the previous eeg special packet ststus to make it full

            if (eegSpecialpacketStatus == false) {
                //  Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  Inside eegSpecialpacketStatus==false case");
                if (eegSpecialPacket[1] == (byte) 0x70) {
                    //   Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  InsideeegSpecialPacket[1] == (byte) 0x70 case");
                    if (eegSpecialPacket[4] == T4_RES_TYPE) {
                        //  Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  Inside eegSpecialpacketStatus==false case inside eegSpecialPacket[4] == T4_RES_TYPE");
                        for (int i = partialPacketFirstPartLength; i < T4_packet_length - 2; i++) {

                            eegSpecialPacket[i] = (byte) 0x20;
                        }
                        eegSpecialPacket[T4_packet_length - 1] = (byte) 0x0d;
                        // Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  Inside eegSpecialpacketStatus==false case eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));
                        eegSpecialpacketStatus = true;
                        //  Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  Inside eegSpecialpacketStatus==false case eegSpecialpacketStatus  set to true");
                        int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, T4_packet_length, channel_nos);
                        eegGraphPacketList.add(localEEGData);

                    } else {
                        eegSpecialpacketStatus = true;
                        // Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  Inside eegSpecialpacketStatus==false case inside else part of eegSpecialPacket[4] == T4_RES_TYPE");
                    }
                } else {
                    eegSpecialpacketStatus = true;
                    // Log.d("Mocxa", "Inside CASE1 Condition: case 1- 1c and 0d  Inside eegSpecialpacketStatus==false case inside else part of eegSpecialPacket[1] == (byte) 0x70");

                }
            }


            int offset = 0;
            int lengthCovered = 0;
            do {
                // Log.d("Mocxa", "Inside CASE1 Condition:do while loop");

                if (offset == bufferLength) break;
                if (offset < bufferLength) {
                    if (databuf[offset] != (byte) 0x1C) {

                        offset = GetSOPOffset(databuf, offset + 1, bufferLength);
                        // Log.d("Mocxa", "Inside CASE1 Condition: Checking [offset]!=(byte) 0x1C AND new Offset when no 1c found");
                        // Log.d("Mocxa", "Inside CASE1 Condition: Checking databuf[offset]!=(byte) 0x1C AND new Offset when no 1c found offset: " + offset);
                        if (offset == -1) {
                            //  Log.d("Mocxa", "Inside CASE1 Condition: Checking [offset]!=(byte) 0x1C AND new Offset when no 1c found offset == -1 check ");
                            offset = bufferLength;
                            //  Log.d("Mocxa", "Inside CASE1 Condition: Checking [offset]!=(byte) 0x1C AND new Offset when no 1c found offset == -1 check  offset = bufferLength offset" + offset);
                        }

                    }
                }
                if (offset == bufferLength) break;

                if (databuf[offset] == (byte) 0x1c) {
                    if (databuf[offset + 1] == (byte) 0x70) {
                        //eeg packet
                        packetSize = (databuf[offset + 3] - (byte) 0x20) * 3 + 6;
                        if (bufferLength < packetSize) break;
                        //Check the RESY/Type to ensure the valid paccket
                        if (databuf[offset + 4] == T4_RES_TYPE) {
                            // Valid packet
                            //  Log.d("Mocxa", "Received 1C and databuf[offset + 1] == (byte) 0x70 Valid Res Type detected");
                            //   Log.d("Mocxa", "Received 1C and databuf[offset + 1] == (byte) 0x70");
                            //   Log.d("Mocxa", "CASE 1 packetSize from packet " + packetSize);
                            //   Log.d("Mocxa", "CASE 1 lengthCovered " + lengthCovered);
                            //  Log.d("Mocxa", "CASE 1 bufferlength " + bufferLength);
                            // Log.d("Mocxa", "Inside type p Condition: case 1- 1c and 0d Valid packet ::packetSize:: " + packetSize);
                            //   Log.d("Mocxa", "Inside type p Condition: case 1- 1c and 0d Valid packet ::Decrypting Data");

                            int prevOffset = offset;
                            offset += packetSize;
                            lengthCovered += packetSize;

                            int newOffset = offset;

                            //received a valid packet with less number of bytes in the packet
                            if (offset > bufferLength) {

                                offset = bufferLength;
                            }

                            if (databuf[offset - 1] != (byte) 0x0d) {
                                newOffset = GetEOPOffset(databuf, offset - 2);
                                if (newOffset == -1) {
                                    offset++;
                                    //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                    //  Log.d("Mocxa", "Inside CASE1 Condition: This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                    //  Log.d("Mocxa", "Inside CASE1 Condition: offset is non zero do loop --else part packetSize; offset Rare case newOffset: " + newOffset);
                                    //  Log.d("Mocxa", "Inside CASE1 Condition: offset is non zero do loop --else part packetSize; offset Rare packetSize: " + packetSize);

                                } else {

                                    if (newOffset == prevOffset) {
                                        offset++;
                                        //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                        // Log.d("Mocxa", "Inside CASE1 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                    } else {
                                        int validEOPposition = (offset - 1) - newOffset;
                                        if (validEOPposition >= packetSize) {
                                            databuf[offset - 1] = (byte) 0x0d;
                                            int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                            eegGraphPacketList.add(localEEGData);

                                            //  Log.d("Mocxa", "Inside CASE1 Condition: Line 777 Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize");
                                            //   Log.d("Mocxa", "Inside CASE1 Condition:Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize validEOPposition  " + validEOPposition);
                                            //  Log.d("Mocxa", "Inside CASE1 Condition:Checking for the new Packet Size and New Offset When 0D Not found  offset validEOPposition offset: " + offset);
                                            //   Log.d("Mocxa", "Inside CASE1 Condition: Checking for the new Packet Size and New Offset When 0D Not found packetSize validEOPposition packetSize: " + packetSize);
                                            // Log.d("Mocxa", "Inside CASE1 Condition: Checking for the new Packet Size and New Offset When 0D Not found packetSize validEOPposition newOffset: " + newOffset);
                                        } else {
                                            packetSize = packetSize - (offset - newOffset);
                                            offset = newOffset;
                                            int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                            eegGraphPacketList.add(localEEGData);

                                            //   Log.d("Mocxa", "Inside CASE1 Condition: Line 786 Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize");
                                            //   Log.d("Mocxa", "Inside CASE1 Condition: Checking for the new Packet Size and New Offset When 0D Not found newOffset: " + newOffset);
                                            //   Log.d("Mocxa", "Inside CASE1 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize  offset: " + offset);
                                            //  Log.d("Mocxa", "Inside CASE1 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of  validEOPposition >= packetSize packetSize: " + packetSize);
                                        }
                                    }
                                }

                            } else {
                                // Log.d("Mocxa", "Inside CASE1 Condition: Else part of databuf[offset - 1] != (byte) 0x0d ");
                                if (prevOffset + packetSize >= bufferLength) {
                                    partialPacketFirstPartLength = (bufferLength - prevOffset);
                                    System.arraycopy(databuf, prevOffset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                                    eegSpecialpacketStatus = false;

                                    offset = bufferLength;
                                    lengthCovered = bufferLength;
                                } else {
                                    int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                    eegGraphPacketList.add(localEEGData);
                                }

                            }
                        } else {
                            //hack to move away  from current 01c
                            offset++;
                            //  Log.d("Mocxa", "CASE 1 - Invalid RES Type detected, incrementing the offset; offset " + offset);
                        }


                    } else if (databuf[offset + 1] == (byte) 0x68 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {

                        //heartbeat packet
                        // Log.d("Mocxa", "CASE 1 Inside heart beat ack Condition -- databuf[offset + 1] == (byte) 0x68 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d ");
                        //  Log.d("Mocxa", "CASE 1 heart beat loop packetSize from packet " + packetSize);
                        // Log.d("Mocxa", "CASE 1 heart beat loop  offset " + offset);
                        //   Log.d("Mocxa", "CASE 1 heart beat loop  lengthCovered " + lengthCovered);
                        //   Log.d("Mocxa", "CASE 1 heart beat loop  bufferlength " + bufferLength);
                        byte status1 = databuf[offset + 2];
                        //   Log.d("Mocxa", "CASE 1 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                        int chkAqg = status1 >> 1 & 1;
                                     /*   if (chkAqg == 0) {
                                        //    Log.d("Mocxa", "CASE 1 Acquisition done, chkAqg: " + chkAqg);
                                            onGoingsStopped = true;
                                        }*/

                        byte status2 = databuf[offset + 3];
                        byte isEvent = (byte) (status2 & (byte) 0x01);
                        //   Log.d("Mocxa", "CASE 1 Checking the Event Status bit in heartbeat loop isEvent == (byte) 0x01" + isEvent);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                            //Log.d("Mocxa", "CASE 1  Updating the Event to DB");
                            /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");
                            Log.d(TAG, "CASE 1  DB Writing the event Completed");*/
                            patientEvents.add("Event");
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        // Log.d("Mocxa", "CASE 1 heartbeat loop  offset incremented by T4_HEARTBEAT_PACKET_SIZE;   offset" + offset);
                        //  Log.d("Mocxa", "CASE 1 heartbeat loop  lengthCovered incremented by T4_HEARTBEAT_PACKET_SIZE" + lengthCovered);


                    } else if (databuf[offset + 1] == (byte) 0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                        // This is a device transmit ACK packet. Check for event

                        // Log.d("Mocxa", "CASE 1 This is a device transmit ACK packet. Check for event");
                        // Log.d("Mocxa", "CASE 1 device transmit ACK packet, packetSize " + packetSize);
                        //  Log.d("Mocxa", "CASE 1 device transmit ACK packet lengthCovered " + lengthCovered);
                        // Log.d("Mocxa", "CASE 1 device transmit ACK packet bufferlength " + bufferLength);

                        byte status1 = databuf[offset + 2];
                        // Log.d("Mocxa", "CASE 1 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                        int chkAqg = status1 >> 1 & 1;
                                       /* if (chkAqg == 0) {
                                          //  Log.d("Mocxa", "CASE 1 Acquisition done, chkAqg: " + chkAqg);
                                            onGoingsStopped = true;
                                        }*/
                        byte status2 = databuf[offset + 3];
                        byte isEvent = (byte) (status2 & (byte) 0x01);

                        // Log.d("Mocxa", "CASE 1 Checking the Event Status bit in device transmit ACK packet isEvent == (byte) 0x01" + isEvent);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                            // Log.d("Mocxa", "CASE 1  Updating the Event to DB");
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");
                            Log.d(TAG, "CASE 1  DB Writing the event Completed");*/
                            patientEvents.add("Event");
                            //Log.d(TAG, "dWriting the event" + databuf);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        // Log.d("Mocxa", "CASE 1 device transmit ACK packet   offset incremented by T4_HEARTBEAT_PACKET_SIZE;   offset" + offset);
                        //  Log.d("Mocxa", "CASE 1 device transmit ACK packet   lengthCovered incremented by T4_HEARTBEAT_PACKET_SIZE" + lengthCovered);
                        /*
                        TODO uncomment
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        Date date = new Date();
                        System.out.println(formatter.format(date));
                        String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :Acknowledge Received \n";
                        Log.i("Mocxa Live", logMsg);
                        fileWriteHandler.logUpdate(logMsg);*/
                        ackEvents.add(new ModelPacketEventAck(status1, status2));


                    } else if (databuf[offset + 1] == (byte) 0x6e && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                        // This is a device transmit NACK packet. Check for event

                        // Log.d("Mocxa", "CASE 1 Nack event loop packetSize from packet " + packetSize);
                        //  Log.d("Mocxa", "CASE 1 Nack event loop  lengthCovered " + lengthCovered);
                        //   Log.d("Mocxa", "CASE 1 Nack event loop  bufferlength " + bufferLength);
                        byte status1 = databuf[offset + 2];
                        //  Log.d("Mocxa", "CASE 1 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                        int chkAqg = status1 >> 1 & 1;
                                       /* if (chkAqg == 0) {
                                         //   Log.d("Mocxa", "CASE 1 Acquisition done, chkAqg: " + chkAqg);
                                            onGoingsStopped = true;
                                        }*/
                        byte status2 = databuf[offset + 3];
                        byte isEvent = (byte) (status2 & (byte) 0x01);
                        //  Log.d("Mocxa", "CASE 1 Checking the Event Status bit in device transmit NACK packet isEvent == (byte) 0x01" + isEvent);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                            //    Log.d("Mocxa", "CASE 1  Updating the Event to DB");
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            //Log.d(TAG, "CASE 1  DB Writing the event Completed");
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;

                        /*
                        TODO uncomment
                           SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        Date date = new Date();
                        System.out.println(formatter.format(date));
                        String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";
                        Log.i("Mocxa Live", logMsg);
                        fileWriteHandler.logUpdate(logMsg);
                        */
                        nackEvents.add(new ModelPacketEventNack(status1, status2));
                        //  Log.d("Mocxa", "CASE 1  device transmit NACK packet offset incremented by T4_HEARTBEAT_PACKET_SIZE;   offset" + offset);
                        //  Log.d("Mocxa", "CASE 1  device transmit NACK packet lengthCovered incremented by T4_HEARTBEAT_PACKET_SIZE" + lengthCovered);

                    } else {

                        offset++;
                        // Log.d(TAG, "Inside CASE1 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                    }

                }

                // Log.d("Mocxa", " Out of if-else CASE1 Condition : length covered GT Test" +lengthCovered);
                // Log.d("Mocxa", " Out of if-else CASE1 Condition:  bufferlength GT test" +bufferLength);

                if (offset == bufferLength) {
                    endOfPacket = true;
                    //Log.d("Mocxa", "CASE 1 lengthCovered == bufferLength condition True that is endOfPacket = true ");
                    //Log.d("Mocxa", "CASE 1 lengthCovered == bufferLength condition Do-While loop completed. ");
                }

            } while (endOfPacket == false);
        } else if (databuf[0] == (byte) 0x1c && databuf[bufferLength - 1] != (byte) 0x0d) {
            //Case2 1c and Not 0d
            // In this case, you have multiple full packets,
            // but the last one is a partial packet
            //  Log.d("Mocxa", "Inside CASE2 Condition: case 1- 1c and NO 0d ");
            //  Log.d("Mocxa", "Inside CASE2 Condition:  == (byte) 0x1c and  != (byte) 0x0d");

            // check the previous eeg special packet ststus to make it full

            if (eegSpecialpacketStatus == false) {
                //  Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside eegSpecialpacketStatus==false case");
                if (eegSpecialPacket[1] == (byte) 0x70) {
                    //  Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside eegSpecialPacket[1] == (byte) 0x70");
                    if (eegSpecialPacket[4] == T4_RES_TYPE) {
                        //   Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside eegSpecialPacket[4] == T4_RES_TYPE  case");
                        for (int i = partialPacketFirstPartLength; i < T4_packet_length - 2; i++) {

                            eegSpecialPacket[i] = (byte) 0x20;
                        }
                        eegSpecialPacket[T4_packet_length - 1] = (byte) 0x0d;
                        // Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside eegSpecialpacketStatus==false case eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));
                        eegSpecialpacketStatus = true;
                        // Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside eegSpecialpacketStatus==false case eegSpecialpacketStatus  set to true");
                        int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, T4_packet_length, channel_nos);
                        eegGraphPacketList.add(localEEGData);


                    } else {
                        eegSpecialpacketStatus = true;
                        //Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside else part  eegSpecialPacket[4] == T4_RES_TYPE  case");
                    }
                } else {
                    eegSpecialpacketStatus = true;
                    // Log.d("Mocxa", "Inside CASE2 Condition: case 2- 1c and No 0d  Inside else part  eegSpecialPacket[1] == (byte) 0x70  case");
                }
            }

            int offset = 0;
            int lengthCovered = 0;
            do {
                // Check for partial packet first to avoid index out of bound error
                //  Log.d("Mocxa", "Inside CASE2 Condition:  Do-While Loop ");

                if (offset == bufferLength) break;

                if (offset < bufferLength) {

                    if (databuf[offset] != (byte) 0x1C) {

                        offset = GetSOPOffset(databuf, offset + 1, bufferLength);

                        // Log.d("Mocxa", "Inside CASE2 Condition: CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND");
                        // Log.d("Mocxa", "Inside CASE2 Condition: CHECKING databuf[offset]!=(byte) 0x1C AND  NEW OFFSET WHEN 1C NOT FOUND newpacketsize" +newpacketsize);
                        // Log.d("Mocxa", "Inside CASE2 Condition: CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND offset" + offset);
                        //Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found packetSize" +packetSize);
                        if (offset == -1) {

                            offset = bufferLength;
                        }

                    }
                }
                if (offset == bufferLength) break;
                //Log.d("Mocxa", "Inside CASE2 Condition: Check for partial packet first to avoid index out of bound error");
                if (databuf[offset] == (byte) 0x1c) {
                    // Log.d("Mocxa", "CASE 2 databuf[offset] == (byte) 0x1c if condition true");
                    int bytesRemaining = bufferLength - offset;
                    // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c Bytes Remaining " + bytesRemaining);

                    if (bytesRemaining <= 3) {
                        partialPacketFirstPartLength = bytesRemaining;
                        // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3  offset::" + offset);
                        // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3  partialPacketFirstPartLength::" + partialPacketFirstPartLength);

                        System.arraycopy(databuf, offset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                        // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3 Array copied partialPacketFirstPartLength");
                        // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3  eegSpecialPacket::" + Arrays.toString(eegSpecialPacket));
                        //setting the EEG Special packet Status to false once the EEG Speecial packet processing not completed
                        eegSpecialpacketStatus = false;

                        offset += partialPacketFirstPartLength;
                        lengthCovered += partialPacketFirstPartLength;
                        //  Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3 partialPacketFirstPartLength " + partialPacketFirstPartLength);

                        //  Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3  offset::" + offset);
                        //  Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside bytesRemaining <=  3  lengthCovered::" + lengthCovered);


                    } else {

                        //Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c  inside bytesRemaining <=  3 case -else part i,e bytesRemaining> 3");
                        packetSize = (databuf[offset + 3] - (byte) 0x20) * 3 + 6;
                        // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c  inside bytesRemaining <=  3 case -else part i,e bytesRemaining> 3 calculated packetSize " + packetSize);
                        // Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside )bytesRemaining <= offset + 3) else part partialPacketFirstPartLength " + partialPacketFirstPartLength);

                        if (offset + packetSize > bufferLength) {
                            //  Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset] == (byte) 0x1c inside )bytesRemaining <= offset + 3) else part offset + packetSize > bufferLength Condition");
                            if (offset + 4 < bufferLength && databuf[offset + 4] == T4_RES_TYPE) {
                                partialPacketFirstPartLength = bufferLength - offset;
                                //   Log.d("Mocxa", "Inside CASE2 Condition: packetSize > bufferLength Condition  bufferLength" + bufferLength);
                                //   Log.d("Mocxa", "Inside CASE2 Condition: packetSize > bufferLength Condition partialPacketFirstPartLength = bufferLength - offset;" + offset);
                                //   Log.d("Mocxa", "Inside CASE2 Condition: packetSize > bufferLength Condition partialPacketFirstPartLength = bufferLength - partialPacketFirstPartLength;" + partialPacketFirstPartLength);


                                System.arraycopy(databuf, offset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                                // Log.d("Mocxa", "Inside CASE2 Condition: packetSize > bufferLength Condition; Copied Partial packet first part to EEG Special packet");
                                //setting the EEG Special packet Status to false once the EEG Speecial packet processing not completed
                                eegSpecialpacketStatus = false;

                                offset += partialPacketFirstPartLength;
                                lengthCovered += partialPacketFirstPartLength;
                                //Log.d("Mocxa", "CASE 2 databuf[offset] == (byte) 0x1c inside )bytesRemaining <= offset + 3) else part offset + packetSize > bufferLength partialPacketFirstPartLength" + packetSize);

                                //Log.d("Mocxa", "CASE 2 databuf[offset] == (byte) 0x1c inside )bytesRemaining <= offset + 3) else part offset + packetSize > bufferLength :offset" + offset);
                                //Log.d("Mocxa", "CASE 2 databuf[offset] == (byte) 0x1c inside )bytesRemaining <= offset + 3) else part offset + packetSize > bufferLength :lengthCovered" + lengthCovered);

                            } else {
                                System.arraycopy(eegZeroPacket, 0, eegSpecialPacket, 0, T4_packet_length);
                                offset++;
                            }

                        } else if (databuf[offset + 1] == (byte) 0x70) {

                            //Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset +1] == (byte) 0x70 Condition");
                            //Checking the RES/TYPE to ensure valid packet
                            if (databuf[offset + 4] == T4_RES_TYPE) {
                                //Log.d("Mocxa", "Inside CASE2 Condition: databuf[offset + 4] == T4_RES_TYPE Valid packet");
                                int prevOffset = offset;
                                offset += packetSize;
                                lengthCovered += packetSize;

                                int newOffset = offset;

                                if (offset > bufferLength) {

                                    offset = bufferLength;
                                }
                                if (databuf[offset - 1] != (byte) 0x0d) {
                                    newOffset = GetEOPOffset(databuf, offset - 2);
                                    if (newOffset == -1) {
                                        offset++;
                                        //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                        // Log.d("Mocxa", "Inside CASE2 Condition: This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                        //  Log.d("Mocxa", "Inside CASE2 Condition: offset is non zero do loop --else part packetSize; offset Rare case" + offset);
                                        //  Log.d("Mocxa", "Inside CASE2 Condition: offset is non zero do loop --else part packetSize; offset Rare packetSize" + packetSize);

                                    } else {

                                        if (newOffset == prevOffset) {
                                            offset++;
                                            //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                            //  Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                        } else {
                                            int validEOPposition = (offset - 1) - newOffset;
                                            if (validEOPposition >= packetSize) {
                                                databuf[offset - 1] = (byte) 0x0d;
                                                int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                                eegGraphPacketList.add(localEEGData);

                                                //  Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize");
                                                //  Log.d("Mocxa", "Inside CASE2 Condition:Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize validEOPposition  " + validEOPposition);
                                                //   Log.d("Mocxa", "Inside CASE2 Condition:Checking for the new Packet Size and New Offset When 0D Not found  offset validEOPposition offset: " + offset);
                                                //   Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found packetSize validEOPposition packetSize: " + packetSize);
                                            } else {
                                                packetSize = packetSize - (offset - newOffset);
                                                offset = newOffset;
                                                int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                                eegGraphPacketList.add(localEEGData);

                                                // Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize");
                                                // Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found newpacketsize" +newpacketsize);
                                                //   Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize  offset: " + offset);
                                                //   Log.d("Mocxa", "Inside CASE2 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of  validEOPposition >= packetSize packetSize: " + packetSize);
                                            }
                                        }
                                    }

                                } else {

                                    // Log.d("Mocxa", "Inside CASE1 Condition: Else part of databuf[offset - 1] != (byte) 0x0d ");
                                    if (prevOffset + packetSize >= bufferLength) {
                                        partialPacketFirstPartLength = (bufferLength - prevOffset);
                                        System.arraycopy(databuf, prevOffset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                                        eegSpecialpacketStatus = false;

                                        offset = bufferLength;
                                        lengthCovered = bufferLength;
                                    } else {
                                        int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                        eegGraphPacketList.add(localEEGData);

                                    }

                                }




                                               /* {
                                                // Log.d("Mocxa", "Inside CASE2 Condition: else part of  databuf[offset - 1] != (byte) 0x0d packetSize: " + packetSize);
                                                int[] localEEGData  = Decrypt_EEG(databuf, prevOffset, packetSize);
                                                EEG_Data = localEEGData;
                                                eegGraphPacketList.add(localEEGData);
                                            }*/
                            } else {

                                offset++;
                                //Log.d("Mocxa", "Inside CASE2 Condition: else part of = T4_RES_TYPE Invalid packet  packet offset" + offset);

                            }

                        } else if (databuf[offset + 1] == (byte) 0x68 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                            // Log.d("Mocxa", "Inside CASE2 Condition Inside heartbeat ack Condition: databuf[offset + 1] == (byte) 0x68 ");
                            // Log.d("Mocxa", "CASE 2 This is a device heartbeat packet. Check for event");
                            //  Log.d("Mocxa", "CASE 2 device transmit heartbeat packet, packetSize: " + packetSize);
                            //   Log.d("Mocxa", "CASE 2 device transmit heartbeat packet lengthCovered: " + lengthCovered);
                            //    Log.d("Mocxa", "CASE 2 device transmit heart packet bufferlength: " + bufferLength);
                            byte status1 = databuf[offset + 2];
                            //Log.d("Mocxa", "CASE 2 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                            int chkAqg = status1 >> 1 & 1;
                                            /*if (chkAqg == 0) {
                                               // Log.d("Mocxa", "CASE 2 Acquisition done, chkAqg: " + chkAqg);
                                                onGoingsStopped = true;
                                            }*/

                            byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                // Log.d("Mocxa", "Inside CASE2 Condition Inside heartbeat  Condition: databuf[offset + 1] == (byte) 0x68 event checking isEvent " + isEvent);
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //   Log.d("Mocxa", "Inside CASE2 Condition Inside heartbeat  Condition: databuf[offset + 1] == (byte) 0x68 event Written to DB");

                                //Log.d(TAG, "dWriting the event" + databuf);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            // Log.d("Mocxa", "CASE 2 heartbeat loop  offset incremented by T4_HEARTBEAT_PACKET_SIZE;   offset" + offset);
                            //  Log.d("Mocxa", "CASE 2 heartbeat loop  lengthCovered incremented by T4_HEARTBEAT_PACKET_SIZE" + lengthCovered);

                        } else if (databuf[offset + 1] == (byte) 0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                            // This is a device transmit ACK packet. Check for event
                            // Log.d("Mocxa", "CASE2 This is a device transmit ACK packet. Check for event");
                            //  Log.d("Mocxa", "CASE 2 device transmit ACK packet, packetSize " + packetSize);
                            // Log.d("Mocxa", "CASE 2 device transmit ACK packet lengthCovered " + lengthCovered);
                            //Log.d("Mocxa", "CASE 2 device transmit ACK packet bufferlength " + bufferLength);
                            byte status1 = databuf[offset + 2];
                            //Log.d("Mocxa", "CASE 2 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                            int chkAqg = status1 >> 1 & 1;
                                           /* if (chkAqg == 0) {
                                              //  Log.d("Mocxa", "CASE 2 Acquisition done, chkAqg: " + chkAqg);
                                                onGoingsStopped = true;
                                            }*/
                            byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                //Log.d("Mocxa", "Inside CASE2 Condition Inside heartbeat  Condition: databuf[offset + 1] == (byte) 0x61 event Writtng  to DB" + isEvent);

                                /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //     Log.d("Mocxa", "Inside CASE2 Condition Inside heartbeat  Condition: databuf[offset + 1] == (byte) 0x61 event Written");

                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d("Mocxa", "CASE 2 device transmit ACK packet   offset incremented by T4_HEARTBEAT_PACKET_SIZE;   offset" + offset);
                            //Log.d("Mocxa", "CASE 2 device transmit ACK packet   lengthCovered incremented by T4_HEARTBEAT_PACKET_SIZE" + lengthCovered);

                        } else if (databuf[offset + 1] == (byte) 0x6e && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                            // This is a device transmit NACK packet. Check for event
                            //Log.d("Mocxa", "CASE 2 Nack event loop packetSize from packet " + packetSize);
                            //Log.d("Mocxa", "CASE 2 Nack event loop  lengthCovered " + lengthCovered);
                            //Log.d("Mocxa", "CASE 2 Nack event loop  bufferlength " + bufferLength);
                            byte status1 = databuf[offset + 2];
                            //Log.d("Mocxa", "CASE 2 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                            int chkAqg = status1 >> 1 & 1;
                                            /*if (chkAqg == 0) {
                                              //  Log.d("Mocxa", "CASE 2 Acquisition done, chkAqg: " + chkAqg);
                                                onGoingsStopped = true;
                                            }*/
                            byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            //Log.d("Mocxa", "Inside CASE2 Condition Inside NACK  Condition: databuf[offset + 1] == (byte) 0x6E event Writtng  to DB" + isEvent);

                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                //  Log.d("Mocxa", "Inside CASE2 Condition Inside NACK  Condition: databuf[offset + 1] == (byte) 0x6E event Writtng  to DB" + isEvent);

                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //    Log.d("Mocxa", "Inside CASE2 Condition Inside NACK  Condition: databuf[offset + 1] == (byte) 0x6E event Written  to DB");

                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            /*
                            TODO uncomment
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            Date date = new Date();
                            System.out.println(formatter.format(date));
                            String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";
                            fileWriteHandler.logUpdate(logMsg);
                            */

                            nackEvents.add(new ModelPacketEventNack(status1, status2));
                            //Log.d("Mocxa", "CASE 2 device transmit NACK packet   offset incremented by T4_HEARTBEAT_PACKET_SIZE;   offset" + offset);
                            //Log.d("Mocxa", "CASE 2 device transmit NACK packet   lengthCovered incremented by T4_HEARTBEAT_PACKET_SIZE" + lengthCovered);
                        } else {

                            offset++;
                            //Log.d(TAG, "Inside CASE2 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                        }
                    } //end of else part for bytes remainnig <=3 check
                } //end of if 1C check

                // Log.d("Mocxa", " Out of if-else CASE2 Condition : length covered GT Test" +lengthCovered);
                // Log.d("Mocxa", " Out of if-else CASE2 Condition:  bufferlength GT test" +bufferLength);
                if (offset == bufferLength) {
                    endOfPacket = true;
                    //Log.d("Mocxa", "CASE 2 lengthCovered == bufferLength condition True that is endOfPacket = true ");
                    //Log.d("Mocxa", "CASE 2 lengthCovered == bufferLength condition Do-While loop completed. ");
                }
            } while (endOfPacket == false); //end of do while for case 2

        } else if (databuf[0] != (byte) 0x1c && databuf[bufferLength - 1] == (byte) 0x0d) {
            // In this case, you have multiple full packets,
            // but the first one is a partial packet
            //case-3  NO 1c znd has 0d

            //Log.d("Mocxa", "Inside CASE3 Condition: ");
            // Log.d("Mocxa", "Inside CASE3 Condition:  != (byte) 0x1c and  == (byte) 0x0d");
            int offset = 0;
            int lengthCovered = 0;
            //Log.d("Mocxa", "Inside CASE 3 Condition:  No 1c and 0d ");

            if (offset == 0) {

                //  Log.d("Mocxa", "Inside CASE 3 Condition:  offset == 0 CHECK ");
                // This is the last part of the special EEG packet
                //Log.d("Mocxa", "Inside CASE 3 Condition: This is the last part of the special EEG packet");
                byte packetType = (byte) 0x0;
                byte numChannels = (byte) 0x0;
                byte status2 = (byte) 0x0;
                //Log.d("Mocxa", "Inside CASE 3 Condition: offset == 0 CHECK partialPacketFirstPartLength " + partialPacketFirstPartLength);

                if (partialPacketFirstPartLength <= 3) {

                    if (checkPacketMetadata(databuf, offset, partialPacketFirstPartLength, bufferLength) == false) {
                        //      Log.d("Mocxa", "Inside CASE3 Inside checkPacketMetadata condition partialPacketFirstPartLength == 1 " + partialPacketFirstPartLength);
                        offset = GetSOPOffset(databuf, offset + 1, bufferLength);
                        //    Log.d("Mocxa", "Inside CASE3 Inside checkPacketMetadata condition offset == 1 " + offset);
                        if (offset == -1) offset = bufferLength;

                    } else {

                        if (partialPacketFirstPartLength == 1) {
                            //      Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 1 " + partialPacketFirstPartLength);

                            packetType = databuf[offset];
                            numChannels = databuf[offset + 2];
                            //    Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 1 packetType " + packetType);
                            //  Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 1 numChannels " + numChannels);
                            // Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 1 offset " + offset);

                            if (packetType == (byte) 0x68 || packetType == (byte) 0x61 || packetType == (byte) 0x6e) {
                                status2 = databuf[offset + 3];

                                //   Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 1  status2 = databuf[offset + 3];  status2" + status2);

                            }

                        } else if (partialPacketFirstPartLength == 2) {
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 2 partialPacketFirstPartLength" + partialPacketFirstPartLength);
                            packetType = eegSpecialPacket[1];
                            numChannels = databuf[offset + 1];
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 2 packetType=eegSpecialPacket[1]" + packetType);
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 2   numChannels = databuf[offset + 1];" + numChannels);
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 2   offset  " + offset);

                            if (packetType == (byte) 0x68 || packetType == (byte) 0x61 || packetType == (byte) 0x6e) {
                                status2 = databuf[offset + 2];
                                //  Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 2  status2 = databuf[offset + 3];  status2" + status2);

                            }

                        } else if (partialPacketFirstPartLength == 3) {
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 3 partialPacketFirstPartLength" + partialPacketFirstPartLength);
                            packetType = eegSpecialPacket[1];
                            numChannels = databuf[offset];

                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 3 packetType=eegSpecialPacket[1]" + packetType);
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 3   numChannels = databuf[offset];" + numChannels);
                            //Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 3    offset " + offset);


                            if (packetType == (byte) 0x68 || packetType == (byte) 0x61 || packetType == (byte) 0x6e) {
                                status2 = databuf[offset + 1];
                                //  Log.d("Mocxa", "Inside CASE3 Inside partialPacketFirstPartLength == 23 status2 = databuf[offset + 3];  status2" + status2);


                            }
                        }

                        if (packetType == (byte) 0x70) {
                            partialPacketLastPartLength = (numChannels - (byte) 0x20) * 3 - partialPacketFirstPartLength;
                            packetSize = partialPacketFirstPartLength + partialPacketLastPartLength;
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 Check" + packetType);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 Check -partialPacketLastPartLength" + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 Check -packetSize" + packetSize);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 Check -offset " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 Check -String copy done" + Arrays.toString(eegSpecialPacket));

                            int prevOffset = offset;
                            offset += partialPacketLastPartLength;
                            lengthCovered += partialPacketLastPartLength;
                            int newOffset = partialPacketLastPartLength;

                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 incremented; prevOffset = offset  prevOffset" + prevOffset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 incremented offset += partialPacketLastPartLength; offset" + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x70 incremented newOffset = partialPacketLastPartLength; newOffset" + offset);


                            //received a valid packet with less number of bytes in the packet
                            if (offset > bufferLength) {

                                //  Log.d("Mocxa", "Inside CASE3 Condition: inside offset - 1 > bufferLength check");

                                offset = bufferLength;
                                //Log.d("Mocxa", "Inside CASE3 Condition: inside offset - 1 > bufferLength check; offset assigned to bufferlength offset = bufferLength; offset " + offset);
                            }
                            if (partialPacketLastPartLength > bufferLength) {
                                partialPacketLastPartLength = bufferLength;
                                //Log.d("Mocxa", "Inside CASE3 Condition:  LINE 1300 else  part : partialPacketFirstPartLength Check 0x70) Inside opartialPacketLastPartLength > bufferLength;  partialPacketLastPartLength = bufferLength; partialPacketLastPartLength: " + partialPacketLastPartLength);
                            }
                            if (databuf[offset - 1] != (byte) 0x0d) {
                                newOffset = GetEOPOffset(databuf, offset - 2);
                                //Log.d("Mocxa", "Inside CASE3 Condition: inside databuf[offset - 1] != (byte) 0x0d check; Came from GetEOPOffset");
                                //Log.d("Mocxa", "Inside CASE3 Condition: inside databuf[offset - 1] != (byte) 0x0d check; Came from GetEOPOffset; newOffset " + newOffset);

                                if (newOffset == -1) {
                                    offset = prevOffset;
                                    //  Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside newOffset ==-1   offset assigned prevOffset; offset " + offset);
                                    // Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside newOffset ==-1   offset assigned prevOffset; Assigning 0x20 to the remaining eegspecial packet");

                                    for (int i = partialPacketFirstPartLength; i < (partialPacketFirstPartLength + partialPacketLastPartLength) - 2; i++) {

                                        eegSpecialPacket[i] = (byte) 0x20;
                                    }
                                    eegSpecialPacket[(partialPacketFirstPartLength + partialPacketLastPartLength) - 1] = (byte) 0x0d;
                                    int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                    eegGraphPacketList.add(localEEGData);

                                    //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                    eegSpecialpacketStatus = true;

                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside newOffset ==-1  partialPacketFirstPartLength: " + partialPacketFirstPartLength);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside newOffset ==-1  partialPacketLastPartLength: " + partialPacketLastPartLength);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside newOffset ==-1  eegspecial packet: " + Arrays.toString(eegSpecialPacket));

                                } else {

                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside else part of newOffset ==-1   offset assigned newOffset");
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside else part of newOffset ==-1 packetSize assigned packetSize - (offset - newOffset)");

                                    packetSize = packetSize - (offset - newOffset);
                                    offset = newOffset;
                                    partialPacketLastPartLength = packetSize - partialPacketFirstPartLength;

                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside else part of newOffset ==-1   offset assigned newOffset  newOffset: " + newOffset);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside else part of newOffset ==-1   offset assigned newOffset  offset: " + offset);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside else part of newOffset ==-1 packetSize assigned packetSize - (offset - newOffset) packetSize:  " + packetSize);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Came from GetEOPOffset; Inside else part of newOffset ==-1 partialPacketLastPartLength = packetSize - partialPacketFirstPartLength;  partialPacketLastPartLength:  " + partialPacketLastPartLength);

                                    System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                    //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                    eegSpecialpacketStatus = true;
                                    int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, packetSize, channel_nos);
                                    eegGraphPacketList.add(localEEGData);


                                }

                            } else {
                                //Log.d("Mocxa", "Inside CASE3 Condition: else part of 0d Not found. here 0d found");

                                offset = partialPacketLastPartLength;
                                System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                eegSpecialpacketStatus = true;
                                int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, packetSize, channel_nos);
                                eegGraphPacketList.add(localEEGData);

                                //Log.d("Mocxa", "Inside CASE3 Condition: else part of 0d Not found. here 0d found offset=partialPacketLastPartLength; offset " + offset);

                            }

                        } else if (packetType == (byte) 0x68) {

                            partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                            //byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            //Log.d("Mocxa", "Inside CASE3 packetType =0x68 Check packetType " + packetType);
                            //Log.d("Mocxa", "Inside CASE3 packetType =0x68 Check -partialPacketLastPartLength" + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE3 packetType =0x68 Check -packetSize" + packetSize);


                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //     Log.d(TAG, "Inside CASE3 0x68 Check  dWriting the event" + databuf);
                            }
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x68 Check");

                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x68 Check offset += T4_HEARTBEAT_PACKET_SIZE; offset:  " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x68 Check  lengthCovered += T4_HEARTBEAT_PACKET_SIZE;; lengthCovered:  " + lengthCovered);


                        } else if (packetType == (byte) 0x61) {
                            partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                            // byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB

                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //   Log.d(TAG, "Inside CASE3 Condition:  packetType =0x61 dWriting the event" + databuf);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d("Mocxa", "Inside CASE3 Condition:  packetType =0x61 Check packetType " + packetType);
                            //Log.d("Mocxa", "Inside CASE3 Condition:  packetType =0x61 Check -partialPacketLastPartLength: " + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE3 Condition:  packetType =0x61 Check offset += T4_HEARTBEAT_PACKET_SIZE;  offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition:  packetType =0x61 Check lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);

                        } else if (packetType == (byte) 0x6e) {
                            partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                            //byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //       Log.d(TAG, "dWriting the event" + databuf);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x6e Check packetType " + packetType);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x6e Check -partialPacketLastPartLength: " + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x6e Check offset += T4_HEARTBEAT_PACKET_SIZE;offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: packetType =0x6e Check lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);

                        } else {

                            offset++;
                            //Log.d(TAG, "Inside CASE3 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                        }


                    }
                } else {
                    if (eegSpecialPacket[1] == (byte) 0x70) {
                        //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; eegSpecialPacket[1]  " + eegSpecialPacket[1]);
                        if (offset < bufferLength) {

                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength ");

                            partialPacketLastPartLength = ((eegSpecialPacket[3] - (byte) 0x20) * 3 - partialPacketFirstPartLength) + 6;
                            packetSize = partialPacketFirstPartLength + partialPacketLastPartLength;
                            int prevOffset = offset;
                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength; partialPacketLastPartLength:  " + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength;  packetSize = partialPacketFirstPartLength + partialPacketLastPartLength:   packetSize: " + packetSize);
                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength;  prevOffset = offset;   prevOffset: " + prevOffset);
                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength;  prevOffset = offset;   offset: " + offset);

                            offset += partialPacketLastPartLength;
                            lengthCovered += partialPacketLastPartLength;

                            int newOffset = partialPacketLastPartLength;

                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength;  offset += partialPacketLastPartLength;;   offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength; lengthCovered += partialPacketLastPartLength;;   lengthCovered: " + lengthCovered);
                            //Log.d("Mocxa", "Inside CASE3 Condition:Else part of partialPacketFirstPartLength <= 3 eegSpecialPacket[1] == (byte) 0x70 condition check; Inside offset < bufferLength; newOffset = partialPacketLastPartLength;   newOffset: " + newOffset);


                            if (offset < bufferLength) {
                                // TODO to fix: java.lang.ArrayIndexOutOfBoundsException: length=916; index=-1
                                //        at com.mocxa.eeg.LiveGraphActivity$ProcessingThread.run(LiveGraphActivity.java:2360)
                                if (databuf[offset - 1] != (byte) 0x0d) {
                                    //Log.d("Mocxa", "Inside CASE3 Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just before going to GetEOPOffset offset   offset: " + offset);

                                    newOffset = GetEOPOffset(databuf, offset - 2);
                                    //Log.d("Mocxa", "Inside CASE3 Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after  going to GetEOPOffset offset   newOffset: " + newOffset);

                                    if (newOffset == -1) {
                                        offset = prevOffset;
                                        //Log.d("Mocxa", "Inside CASE3 Condition: line 1353databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after going to GetEOPOffset newOffset == -1   newOffset: " + newOffset);

                                        for (int i = partialPacketFirstPartLength; i < (partialPacketFirstPartLength + partialPacketLastPartLength) - 2; i++) {

                                            eegSpecialPacket[i] = (byte) 0x20;
                                        }
                                        eegSpecialPacket[(partialPacketFirstPartLength + partialPacketLastPartLength) - 1] = (byte) 0x0d;
                                        //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                        eegSpecialpacketStatus = true;

                                        //Log.d("Mocxa", "Inside CASE3 Line No 1360Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after going to GetEOPOffset eegSpecialPacket added with 0x20: eegSpecialPacket " + Arrays.toString(eegSpecialPacket));

                                        int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                        eegGraphPacketList.add(localEEGData);

                                    } else {
                                                        /* offset = offset - partialPacketLastPartLength + newpacketsize;
                                                            packetSize = newpacketsize;*/
                                        //Log.d("Mocxa", "Inside CASE3 Line No 1366 Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after going to GetEOPOffset eegSpecialPacket else part of newOffset == -1 check");

                                        packetSize = packetSize - (offset - newOffset);
                                        offset = newOffset;
                                        partialPacketLastPartLength = packetSize - partialPacketFirstPartLength;
                                        //Log.d("Mocxa", "Inside CASE3 Line No 1371 Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after going to GetEOPOffset eegSpecialPacket else part of newOffset == -1 check; packetSize = packetSize - (offset - newOffset); packetSize: " + packetSize);
                                        //Log.d("Mocxa", "Inside CASE3 Line No 1372 Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after going to GetEOPOffset eegSpecialPacket else part of newOffset == -1 check;  offset = newOffset; offset: " + offset);
                                        //Log.d("Mocxa", "Inside CASE3 Line No 1373 Condition:databuf[offset - 1] != (byte) 0x0d condition check; Inside offset < bufferLength; just after going to GetEOPOffset eegSpecialPacket else part of newOffset == -1 check;  partialPacketLastPartLength = packetSize - partialPacketFirstPartLength;; partialPacketLastPartLength: " + offset);


                                        System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                        //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                        eegSpecialpacketStatus = true;
                                        int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                        eegGraphPacketList.add(localEEGData);


                                    }

                                } else {
                                    offset = partialPacketLastPartLength;
                                    System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                    //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                    eegSpecialpacketStatus = true;
                                    int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                    eegGraphPacketList.add(localEEGData);

                                    //Log.d("Mocxa", "Inside CASE3 Line No 1384 Condition: in else part of databuf[offset - 1] != (byte) 0x0d condition check; offset = partialPacketLastPartLength;  offset: " + offset);
                                    //Log.d("Mocxa", "Inside CASE3 Line No 1385 Condition: in else part of databuf[offset - 1] != (byte) 0x0d condition check; array copied amd sent to eeg decrypt");


                                }

                            } else {
                                for (int i = partialPacketFirstPartLength; i < (partialPacketFirstPartLength + partialPacketLastPartLength) - 2; i++) {

                                    eegSpecialPacket[i] = (byte) 0x20;
                                }
                                eegSpecialPacket[(partialPacketFirstPartLength + partialPacketLastPartLength) - 1] = (byte) 0x0d;
                                //setting the EEG Special packet Status to true once the EEG Speecial packet processing  completed
                                eegSpecialpacketStatus = true;
                                int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                eegGraphPacketList.add(localEEGData);


                                offset = bufferLength;
                                //Log.d("Mocxa", "Inside CASE3 Line No 1398 Condition: in else part of  if (offset < bufferLength) condition check;  offset = bufferLength;  offset: " + offset);
                                //Log.d("Mocxa", "Inside CASE3 Line No 1398 Condition: in else part of  if (offset < bufferLength) condition check;  eegSpecialPacket = : " + Arrays.toString(eegSpecialPacket));

                            }

                        }
                    } else if (eegSpecialPacket[1] == (byte) 0x68) {
                        partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                        byte status = eegSpecialPacket[3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            //  Log.d(TAG, "dWriting the event" + databuf);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else part : eegSpecialPacket [offset + 1] Check 0x68:packetType " + packetType);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : eegSpecialPacket [offset + 1 Check 0x68:partialPacketFirstPartLength " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE3 Condition: coffset=0 else part : partialPacketLastPartLength Check 0x68:partialPacketLastPartLength " + partialPacketLastPartLength);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x68 :status " + status);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x68 :offset " + offset);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x68 :lengthCovered " + lengthCovered);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x68 :eegSpecialPacket " + Arrays.toString(eegSpecialPacket));


                    } else if (eegSpecialPacket[1] == (byte) 0x61) {

                        partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                        byte status = eegSpecialPacket[3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            // Log.d(TAG, "dWriting the event" + databuf);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;

                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else part : eegSpecialPacket [offset + 1] Check 0x61:packetType " + packetType);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : eegSpecialPacket [offset + 1 Check 0x61:partialPacketFirstPartLength " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else part : partialPacketLastPartLength Check 0x61:partialPacketLastPartLength " + partialPacketLastPartLength);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x61 :status " + status);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x61 :offset " + offset);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x61 :lengthCovered " + lengthCovered);

                    } else if (eegSpecialPacket[1] == (byte) 0x6E) {
                        partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                        byte status = eegSpecialPacket[3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            //   Log.d(TAG, "dWriting the event" + databuf);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else part : eegSpecialPacket [offset + 1] Check 0x6E:packetType " + packetType);
                        //Log.d("Mocxa", "Inside CASE3 Condition:  offset=0 else  part : eegSpecialPacket [offset + 1 Check 0x6E:partialPacketFirstPartLength " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE3 Condition:  offset=0 else part : partialPacketLastPartLength Check 0x6E:partialPacketLastPartLength " + partialPacketLastPartLength);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x6E :status " + status);
                        //Log.d("Mocxa", "Inside CASE3 Condition: offset=0 else  part : partialPacketFirstPartLength Check0x6E :offset " + offset);
                        //Log.d("Mocxa", "Inside CASE3 Condition:  offset=0 else  part : partialPacketFirstPartLength Check0x6E :lengthCovered " + lengthCovered);
                        /*TODO uncomment
                           SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        Date date = new Date();
                        System.out.println(formatter.format(date));
                        String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";

                        fileWriteHandler.logUpdate(logMsg);*/

                        nackEvents.add(new ModelPacketEventNack(eegSpecialPacket[2], status));
                    } else {

                        offset++;
                        //Log.d(TAG, "Inside CASE3 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                    }


                }
            }
            // Do the same as the first condition of 0x1C && 0x0D
            do {
                if (offset == bufferLength) break;
                //Log.d("Mocxa", "Inside CASE3 Condition: Inside do-while");
                //Log.d("Mocxa", "Inside CASE3 Condition: Before  offset < bufferLength condition; offset: " + offset);
                //Log.d("Mocxa", "Inside CASE3 Condition: Before  offset < bufferLength condition; bufferLength:   " + bufferLength);

                if (offset < bufferLength) {
                    //Log.d("Mocxa", "Inside CASE3 Condition: inside offset < bufferLength ");
                    if (databuf[offset] != (byte) 0x1C) {


                        //Log.d("Mocxa", "Inside CASE3 Condition: (offset < bufferLength)  check for 1c databuf[offset] != (byte) 0x1C; Inside if condition, 1c not found");
                        //Log.d("Mocxa", "Inside CASE3 Condition: LINE NO1480 CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND before GetSOPOffset offset:  " + offset);

                        offset = GetSOPOffset(databuf, offset + 1, bufferLength);

                        //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1484 CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND AFTER GetSOPOffset offset:  " + offset);
                        //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1485 CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND offset" + offset);

                        if (offset == -1) {

                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1490 CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND AFTER GetSOPOffset offset==-1  offset: " + offset);

                            offset = bufferLength;

                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1490 CHECKING databuf[offset]!=(byte) 0x1C AND NEW OFFSET WHEN 1C NOT FOUND AFTER GetSOPOffset  offset = bufferLength;  offset: " + offset);


                        }
                    }
                }
                if (offset == bufferLength) break;

                if (databuf[offset] == (byte) 0x1c) {
                    //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1502 databuf[offset] == (byte) 0x1c check   offset : " + offset);

                    if (databuf[offset + 1] == (byte) 0x70) {
                        // Valid packet
                        //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1506 (databuf[offset + 1] == (byte) 0x70check   offset : " + offset);

                        packetSize = (databuf[offset + 3] - (byte) 0x20) * 3 + 6;
                        //Log.d("Mocxa", "nside CASE3 Condition: LINE 1506 (databuf[offset + 1] == (byte) 0x70 check. Packet size calculated packetSize: " + packetSize);
                        if (bufferLength < packetSize) break;
                        if ((offset + 4 < bufferLength) && (databuf[offset + 4] == T4_RES_TYPE)) {
                            //Log.d("Mocxa", "Inside CASE3 Condition: databuf[offset + 4] == T4_RES_TYPE Valid packet");

                            int prevOffset = offset;
                            offset += packetSize;
                            lengthCovered += packetSize;

                            int newOffset = offset;

                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1519 (databuf[offset + 4] == T4_RES_TYPE  check   offset : " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1520 (databuf[offset + 4] == T4_RES_TYPE  check   prevOffset = offset;  prevOffset: " + prevOffset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1521 (databuf[offset + 4] == T4_RES_TYPE  check   offset += packetSize;;  offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1522 (databuf[offset + 4] == T4_RES_TYPE  check  lengthCovered += packetSize;  lengthCovered: " + lengthCovered);
                            //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1523 (databuf[offset + 4] == T4_RES_TYPE  check   newOffset = offset; newOffset: " + newOffset);


                            //received a valid packet with less number of bytes in the packet
                            if (offset - 1 > bufferLength) {
                                //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1528 (databuf[offset + 4] == T4_RES_TYPE  check  offset - 1 > bufferLength; bufferLength:" + bufferLength);
                                offset = bufferLength;
                                //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1530 (databuf[offset + 4] == T4_RES_TYPE  check  offset - 1 > bufferLength; offset = bufferLength; offset: " + offset);

                            }
                            if (databuf[offset - 1] != (byte) 0x0d) {
                                //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1534 databuf[offset - 1] != (byte) 0x0d  check  newOffset = GetEOPOffset(databuf, offset - 2); offset: " + offset);
                                newOffset = GetEOPOffset(databuf, offset - 2);
                                //Log.d("Mocxa", "Inside CASE3 Condition: LINE 1536 databuf[offset - 1] != (byte) 0x0d  check  newOffset = GetEOPOffset(databuf, offset - 2); newOffset: " + newOffset);

                                if (newOffset == -1) {
                                    offset++;
                                    //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Line 1546 This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: line 1547 offset is non zero do loop --else part packetSize; offset Rare case offset: " + offset);
                                    //Log.d("Mocxa", "Inside CASE3 Condition: Line 1548 offset is non zero do loop --else part packetSize; offset Rare packetSize: " + packetSize);

                                } else {

                                    if (newOffset == prevOffset) {
                                        offset++;
                                        //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                        //Log.d("Mocxa", "Inside CASE3 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                    } else {
                                        int validEOPposition = (offset - 1) - newOffset;
                                        if (validEOPposition >= packetSize) {
                                            databuf[offset - 1] = (byte) 0x0d;
                                            int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                            eegGraphPacketList.add(localEEGData);

                                            //Log.d("Mocxa", "Inside CASE3 Condition: Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize");
                                            //Log.d("Mocxa", "Inside CASE3 Condition:Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize validEOPposition  " + validEOPposition);
                                            //Log.d("Mocxa", "Inside CASE3 Condition:Checking for the new Packet Size and New Offset When 0D Not found  offset validEOPposition offset: " + offset);
                                            //Log.d("Mocxa", "Inside CASE3 Condition: Checking for the new Packet Size and New Offset When 0D Not found packetSize validEOPposition packetSize: " + packetSize);
                                        } else {
                                            packetSize = packetSize - (offset - newOffset);
                                            offset = newOffset;
                                            int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                            eegGraphPacketList.add(localEEGData);
                                            //Log.d("Mocxa", "Inside CASE3 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize");
                                            //Log.d("Mocxa", "Inside CASE3 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize  offset: " + offset);
                                            //Log.d("Mocxa", "Inside CASE3 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of  validEOPposition >= packetSize packetSize: " + packetSize);
                                        }
                                    }
                                }

                            } else {
                                // Log.d("Mocxa", "Inside CASE1 Condition: Else part of databuf[offset - 1] != (byte) 0x0d ");
                                if (prevOffset + packetSize >= bufferLength) {
                                    partialPacketFirstPartLength = (bufferLength - prevOffset);
                                    System.arraycopy(databuf, prevOffset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                                    eegSpecialpacketStatus = false;

                                    offset = bufferLength;
                                    lengthCovered = bufferLength;
                                } else {
                                    int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                    eegGraphPacketList.add(localEEGData);

                                }

                            }
                                            /*{
                                            int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize);
                                            EEG_Data = localEEGData;
                                            eegGraphPacketList.add(localEEGData);
                                            //Log.d("Mocxa", "Inside CASE3 LINE 1578 inside else part of (databuf[offset - 1] != (byte) 0x0d) ;  decrypt EEG");
                                        }*/
                        } else {
                            //hack to getawy from 1c check
                            offset++;
                            //Log.d("Mocxa", "Inside CASE3 Condition Line : 1583:else part of  databuf[offset + 4] == T4_RES_TYPE Invalid  packet offset++; offset: " + offset);

                        }


                    } else if (databuf[offset + 1] == (byte) 0x68 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                        //Log.d("Mocxa", "Inside CASE3 Condition Line : 1582  Inside databuf[offset + 1] ==(byte)  0x68 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d heartbeat ack Condition");
                        byte status1 = databuf[offset + 2];
                        //Log.d("Mocxa", "CASE 3 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                        int chkAqg = status1 >> 1 & 1;
                                       /* if (chkAqg == 0) {
                                            //Log.d("Mocxa", "CASE 3 Acquisition done, chkAqg: " + chkAqg);
                                            onGoingsStopped = true;
                                        }*/
                        byte status = databuf[offset + 3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            //     Log.d(TAG, "Inside CASE3 Condition Line : 1589 dBWriting the event status: " + status);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d(TAG, "Inside CASE3 Condition Line : 0x68 offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                        //Log.d(TAG, "Inside CASE3 Condition Line : 0x68 lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);

                    } else if (databuf[offset + 1] == (byte) 0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {

                        //Log.d("Mocxa", "Inside CASE3 Condition Line : 1598  Inside databuf[offset + 1] ==(byte)  0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d ACK PACKET Condition");
                        // This is a device transmit ACK packet. Check for event
                        byte status1 = databuf[offset + 2];
                        //Log.d("Mocxa", "CASE 3 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                        int chkAqg = status1 >> 1 & 1;
                                       /* if (chkAqg == 0) {
                                            //Log.d("Mocxa", "CASE 3 Acquisition done, chkAqg: " + chkAqg);
                                            onGoingsStopped = true;
                                        }*/
                        byte status = databuf[offset + 3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            //   Log.d(TAG, "Inside CASE3 Condition Line : 1605  dBWriting the event status: " + status);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d(TAG, "Inside CASE3 Condition Line :1609 0x61 offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                        //Log.d(TAG, "Inside CASE3 Condition Line : 1610 0x61 lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);
                        //Log.d("Mocxa", "case 3  else part of This is a device transmit ACK packet. Check for event increased lengthcovered GT test" + lengthCovered);

                    } else if (databuf[offset + 1] == (byte) 0x6e && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                        // This is a device transmit NACK packet. Check for event
                        //Log.d("Mocxa", "Inside CASE3 Condition Line : 1615  Inside databuf[offset + 1] ==(byte)  0x6E && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d NACK PACKET Condition");
                        byte status1 = databuf[offset + 2];
                        //Log.d("Mocxa", "CASE 3 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                        int chkAqg = status1 >> 1 & 1;
                                      /*  if (chkAqg == 0) {
                                            //Log.d("Mocxa", "CASE 3 Acquisition done, chkAqg: " + chkAqg);
                                            onGoingsStopped = true;
                                        }*/
                        byte status = databuf[offset + 3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            //     Log.d(TAG, "Inside CASE3 Condition Line : 1622 0x6E  dBWriting the event status: " + status);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d(TAG, "Inside CASE3 Condition Line : 1626 0x61 offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                        //Log.d(TAG, "Inside CASE3 Condition Line : 1627 0x61 lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);
                        //Log.d("Mocxa", "case 3 last else part of This is a device transmit NACK packet. Check for event , increased lengthcovered GT test" + lengthCovered);
                        /*
                        TODO uncomment
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        Date date = new Date();
                        System.out.println(formatter.format(date));
                        String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";
                        fileWriteHandler.logUpdate(logMsg);
*/
                        nackEvents.add(new ModelPacketEventNack(status1, status));

                    } else {

                        offset++;
                        //Log.d(TAG, "Inside CASE3 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                    }
                }

                if (offset == bufferLength) {
                    endOfPacket = true;
                    //Log.d(TAG, "Inside CASE3 Condition Line : 1634  inside offset == bufferLength check to end the do while loop offset: " + offset);
                    //Log.d(TAG, "Inside CASE3 Condition Line : 1635  inside offset == bufferLength check to end the do while loop bufferLength: " + bufferLength);
                    //Log.d(TAG, "Inside CASE3 Condition Line : 1635; endOfPaket=true");
                }
            } while (endOfPacket == false);  // end of do while loop of case 3
            //Log.d(TAG, "End Of Case 3");
        } else if (databuf[0] != (byte) 0x1c && databuf[bufferLength - 1] != (byte) 0x0d) { //end of case 3
            // In this case, you have multiple full packets,
            // but the first one is a partial packet and the last ont is a partial packet
            //Log.d("Mocxa", "CASE4 Condition: case 4- No 1c and No 0d ");
            //case-4 no 1c no 0d
            int offset = 0;
            int lengthCovered = 0;
            //Log.d("Mocxa", "Inside CASE4 Condition: case 4- No 1c and No 0d ");

            if (offset == 0) {


                // This is the last part of the special EEG packet
                byte packetType = (byte) 0x0;
                byte numChannels = (byte) 0x0;
                byte status2 = (byte) 0x0;
                //Log.d("Mocxa", "Inside CASE4 Condition: offset=0 if part : partialPacketFirstPartLength " + partialPacketFirstPartLength);
                if (partialPacketFirstPartLength <= 3) {

                    if (checkPacketMetadata(databuf, offset, partialPacketFirstPartLength, bufferLength) == false) {

                        //Log.d("Mocxa", "Inside CASE4 Inside checkPacketMetadata condition partialPacketFirstPartLength == 1 " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE4 Inside checkPacketMetadata condition offset=GetSOPOffset(databuf, offset + 1, bufferLength);  bufferLength:" + bufferLength);
                        //Log.d("Mocxa", "Inside CASE4 Inside checkPacketMetadata condition offset=GetSOPOffset(databuf, offset + 1, bufferLength);  offset:" + offset);

                        offset = GetSOPOffset(databuf, offset + 1, bufferLength);
                        //Log.d("Mocxa", "Inside CASE4 Inside checkPacketMetadata condition offset=GetSOPOffset(databuf, offset + 1, bufferLength);  offset:" + offset);
                        if (offset == -1) {
                            //Log.d("Mocxa", "Inside CASE4 Inside offset == -1 check;  offset:" + offset);
                            offset = bufferLength;
                            //Log.d("Mocxa", "Inside CASE4 Inside offset == -1 check; offset = bufferLength; offset: " + offset);
                        }
                    } else {
                        //Log.d("Mocxa", "Inside CASE4 Line: 1674 Inside else part of checkPacketMetadata condition check; partialPacketFirstPartLength:  " + partialPacketFirstPartLength);
                        if (partialPacketFirstPartLength == 1) {
                            //Log.d("Mocxa", "Inside CASE4 Condition:  if (partialPacketFirstPartLength == 1) partialPacketFirstPartLength Check" + partialPacketFirstPartLength);
                            packetType = databuf[offset];
                            numChannels = databuf[offset + 2];
                            //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset=0 if part : partialPacketFirstPartLength==1 Check :packetType " + packetType);
                            //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset=0 if part : Line 1680 partialPacketFirstPartLength==1 Check :numChannels " + numChannels);


                            if (packetType == (byte) 0x68 || packetType == (byte) 0x61 || packetType == (byte) 0x6e) {
                                status2 = databuf[offset + 3];
                                //Log.d("Mocxa", "Inside CASE4 Condition:Line 1685-  partialPacketFirstPartLength==1 Check status2: " + status2);
                            }


                        } else if (partialPacketFirstPartLength == 2) {
                            //Log.d("Mocxa", "Inside CASE4 Condition: case 4- partialPacketFirstPartLength == 2  Check partialPacketFirstPartLength: " + partialPacketFirstPartLength);
                            packetType = eegSpecialPacket[1];
                            numChannels = databuf[offset + 1];
                            //Log.d("Mocxa", "Inside CASE4 Condition: offset=0 if part : partialPacketFirstPartLength==2 Check : packetType = eegSpecialPacket[1]; packetType " + packetType);
                            //Log.d("Mocxa", "Inside CASE4 Condition: offset=0 if part : partialPacketFirstPartLength==2 Check :numChannels = databuf[offset + 1];  numChannels " + numChannels);

                            if (packetType == (byte) 0x68 || packetType == (byte) 0x61 || packetType == (byte) 0x6e) {
                                status2 = databuf[offset + 2];
                                //Log.d("Mocxa", "Inside CASE4 Condition: case 4-  partialPacketFirstPartLength==2 Check status2: " + status2);
                            }
                        } else if (partialPacketFirstPartLength == 3) {
                            //Log.d("Mocxa", "Inside CASE4 Condition: case 4-  partialPacketFirstPartLength==3 Check partialPacketFirstPartLength:  " + partialPacketFirstPartLength);
                            packetType = eegSpecialPacket[1];
                            numChannels = databuf[offset];
                            //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset=0 if part : partialPacketFirstPartLength==3 Check :packetType = eegSpecialPacket[1];  packetType " + packetType);
                            //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset=0 if part : partialPacketFirstPartLength==3 Check :  numChannels = databuf[offset];  numChannels " + numChannels);

                            if (packetType == (byte) 0x68 || packetType == (byte) 0x61 || packetType == (byte) 0x6e) {
                                status2 = databuf[offset + 1];
                                //Log.d("Mocxa", "Inside CASE4 Condition: case 4-  partialPacketFirstPartLength==3 Check status2:  " + status2);
                            }
                        }

                        if (packetType == (byte) 0x70) {
                            if (eegSpecialPacket[4] == T4_RES_TYPE) {
                                //Log.d("Mocxa", "Inside CASE4 line 1792 packetType =0x70 Check eegSpecialPacket[offset + 4] == T4_RES_TYPE  T4_RES_TYPE: " + T4_RES_TYPE);
                                partialPacketLastPartLength = (numChannels - (byte) 0x20) * 3 - partialPacketFirstPartLength;
                                //Log.d("Mocxa", "Inside CASE4 packetType =0x70 Check partialPacketLastPartLength: " + partialPacketLastPartLength);

                                int prevOffset = offset;

                                offset += partialPacketLastPartLength;
                                lengthCovered += partialPacketLastPartLength;
                                int newOffset = partialPacketLastPartLength;
                                //received a valid packet with less number of bytes in the packet
                                //Log.d("Mocxa", "Inside CASE4 Line No 1730  packetType =0x70 Check prevOffset = offset; prevOffset:  " + prevOffset);
                                //Log.d("Mocxa", "Inside CASE4 Line No 1731  packetType =0x70 Check  offset += partialPacketLastPartLength;  offset:  " + offset);
                                //Log.d("Mocxa", "Inside CASE4 Line No 1732  packetType =0x70 Check  lengthCovered += partialPacketLastPartLength;  lengthCovered:  " + lengthCovered);
                                //Log.d("Mocxa", "Inside CASE4 Line No 1733  packetType =0x70 Check  newOffset = partialPacketLastPartLength; = partialPacketLastPartLength;  newOffset:  " + newOffset);
                                //Log.d("Mocxa", "Inside CASE4 Line No 1734  packetType =0x70 Check  bufferLength:  " + bufferLength);

                                if (offset > bufferLength) {
                                    // Log.d("Mocxa", "Inside CASE4 Line No 1735  packetType =0x70 Check  offset - 1 > bufferLength:  " + true);

                                    offset = bufferLength;
                                    //Log.d("Mocxa", "Inside CASE4 Line No 1735  packetType =0x70 Check  offset - 1 > bufferLength;   offset = bufferLength;  offset:  " + offset);

                                }
                                if (databuf[offset - 1] != (byte) 0x0d) {
                                    newOffset = GetEOPOffset(databuf, offset - 2);
                                    //Log.d("Mocxa", "Inside CASE4 Line No 1745  databuf[offset - 1] != (byte) 0x0d)  Check   newOffset = GetEOPOffset(databuf, offset - 2); newOffset: " + newOffset);
                                    if (newOffset == -1) {
                                        offset = prevOffset;
                                        //Log.d("Mocxa", "Inside CASE4 Line No 1748  databuf[offset - 1] != (byte) 0x0d)  Check  (newOffset == -1) check; newOffset: " + newOffset);
                                        //Log.d("Mocxa", "Inside CASE4 Line No 1749  databuf[offset - 1] != (byte) 0x0d)  Check  (newOffset == -1) check;  offset = prevOffset;  offset: " + offset);


                                        for (int i = partialPacketFirstPartLength; i < (partialPacketFirstPartLength + partialPacketLastPartLength) - 2; i++) {

                                            eegSpecialPacket[i] = (byte) 0x20;
                                        }
                                        eegSpecialPacket[(partialPacketFirstPartLength + partialPacketLastPartLength) - 1] = (byte) 0x0d;
                                        //Log.d("Mocxa", "Inside CASE4 Line No 1757  databuf[offset - 1] != (byte) 0x0d)  Check  (newOffset == -1) check;  EEG Special packet updated with 0x20: eegSpecialPacket:  " + Arrays.toString(eegSpecialPacket));

                                        int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                        eegGraphPacketList.add(localEEGData);
                                        //setting the EEG Special packet Status to true once the EEG Speecial packet processing completed
                                        eegSpecialpacketStatus = true;


                                    } else {
                                        //Log.d("Mocxa", "Inside CASE4 Line No 1761  else part of (newOffset == -1) check;  packetSize = packetSize - (offset - newOffset)");
                                        packetSize = packetSize - (offset - newOffset);
                                        //Log.d("Mocxa", "Inside CASE4 Line No 1763  else part of (newOffset == -1) check;  packetSize = packetSize - (offset - newOffset) packetSize: " + packetSize);
                                        offset = newOffset;
                                        //Log.d("Mocxa", "Inside CASE4 Line No 1765  else part of (newOffset == -1) check;  offset = newOffset; offset: " + offset);
                                        System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                        int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                        eegGraphPacketList.add(localEEGData);
                                        //setting the EEG Special packet Status to true once the EEG Speecial packet processing completed
                                        eegSpecialpacketStatus = true;


                                        //Log.d("Mocxa", "Inside CASE4 Line No 1770  else part of (newOffset == -1) eeg special packet copied eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));
                                    }

                                } else {

                                    //Log.d("Mocxa", "Inside CASE4 Line No 1777  else part of (databuf[offset - 1] != (byte) 0x0d: offset: " + offset);
                                    offset = partialPacketLastPartLength;
                                    //Log.d("Mocxa", "Inside CASE4 Line No 1777  else part of (databuf[offset - 1] != (byte) 0x0d:  offset=partialPacketLastPartLength; offset: " + offset);
                                    System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                    int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                    eegGraphPacketList.add(localEEGData);
                                    //setting the EEG Special packet Status to true once the EEG Speecial packet processing completed
                                    eegSpecialpacketStatus = true;
                                    //Log.d("Mocxa", "Inside CASE4 Line No 1780  else part of (databuf[offset - 1] != (byte) 0x0d:  EEG Special packet copied eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));
                                }
                            } else {
                                offset++;
                                eegSpecialpacketStatus = true;
                                //Log.d("Mocxa", "Inside CASE4 line 1865 packetType =0x70 else part of  eegSpecialPacket[offset + 4] == T4_RES_TYPE  T4_RES_TYPE: " + T4_RES_TYPE);
                                //Log.d("Mocxa", "Inside CASE4 line 1866 packetType =0x70 else part of  eegSpecialPacket[offset + 4] == T4_RES_TYPE  offset: " + offset);
                                //Log.d("Mocxa", "Inside CASE4 line 1866 packetType =0x70 else part of  eegSpecialPacket[offset + 4] == T4_RES_TYPE  eegSpecialpacketStatus: " + eegSpecialpacketStatus);
                            }


                        } else if (packetType == (byte) 0x68) {
                            //Log.d("Mocxa", "Inside CASE4 Line No 1784  packetType == (byte) 0x68 ; offset: " + offset);
                            partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                            //byte status2 = databuf[offset + 3];

                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                // Log.d(TAG, "Inside CASE4 Line No 1791  packetType == (byte) 0x68 DB Writing the event status2:" + status2);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x68 Check packetType: " + packetType);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x68 Check -partialPacketLastPartLength: " + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x68 Check -Event written Status: " + status2);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x68 Check offset += T4_HEARTBEAT_PACKET_SIZE;  offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE4 Condition:  packetType =0x68 Check lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);

                        } else if (packetType == (byte) 0x61) {
                            //Log.d("Mocxa", "Inside CASE4 Line No 1802  packetType == (byte) 0x61 ; offset: " + offset);
                            partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                            // byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB

                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                // Log.d(TAG, "Inside CASE4 Line No 1809  packetType == (byte) 0x61 ; DB Writing the event status2: " + status2);
                            }

                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;

                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x61 Check packetType: " + packetType);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x61 Check partialPacketLastPartLength" + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x61 Check -Event written Status: " + status2);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x61 Check offset += T4_HEARTBEAT_PACKET_SIZE;  offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE4 Condition:  packetType =0x61 Check   lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);


                        } else if (packetType == (byte) 0x6e) {
                            //Log.d("Mocxa", "Inside CASE4 Line No 1823  packetType == (byte) 0x6E ; offset: " + offset);
                            partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                            //byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB

                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //   Log.d(TAG, "Inside CASE4 Line No 1830  packetType == (byte) 0x6E ; DB Writing the event status2" + status2);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x6E Check packetType: " + packetType);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x6E Check -partialPacketLastPartLength: " + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x6E Check -Event written Status: " + status2);
                            //Log.d("Mocxa", "Inside CASE4 Condition: packetType =0x6E Check  offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE4 Condition:  packetType =0x6E Check lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);
                            /*

                            TODO uncomment
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            Date date = new Date();
                            System.out.println(formatter.format(date));
                            String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";
                            fileWriteHandler.logUpdate(logMsg);
                            */

                            nackEvents.add(new ModelPacketEventNack((byte)0xff, status2));

                        } else {

                            offset++;
                            //Log.d(TAG, "Inside CASE4 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                        }


                    }
                    //Log.d("Mocxa", "Inside CASE4  LINE : 1844 End of partialPacketFirstPartLength <= 3 check ");
                } else {
                    //Log.d("Mocxa", "Inside CASE4  LINE : 1847  else part of  (partialPacketFirstPartLength <= 3) check ");
                    if (eegSpecialPacket[1] == (byte) 0x70) {
                        //Log.d("Mocxa", "Inside CASE4  LINE : 1849  else part of  (partialPacketFirstPartLength <= 3) check; Inside eegSpecialPacket[1] == (byte) 0x70 check ");
                        if (offset < bufferLength) {
                            partialPacketLastPartLength = ((eegSpecialPacket[3] - (byte) 0x20) * 3 - partialPacketFirstPartLength) + 6;
                            packetSize = partialPacketFirstPartLength + partialPacketLastPartLength;

                            //Log.d("Mocxa", "Inside CASE4 Condition:  else  part : partialPacketFirstPartLength Check 0x70):eegSpecialPacket before copying " + Arrays.toString(eegSpecialPacket));
                            //Log.d("Mocxa", "Inside CASE4 Condition:  else  part : partialPacketFirstPartLength Check 0x70)partialPacketLastPartLength:  " + partialPacketLastPartLength);
                            //Log.d("Mocxa", "Inside CASE4 Condition:  else  part : partialPacketFirstPartLength Check 0x70) offset;" + offset);
                            //Log.d("Mocxa", "Inside CASE4 Condition partelse  part : partialPacketFirstPartLength Check 0x70) packetSize;" + packetSize);

                            int prevOffset = offset;
                            offset += partialPacketLastPartLength;
                            lengthCovered += partialPacketLastPartLength;

                            int newOffset = partialPacketLastPartLength;

                            //Log.d("Mocxa", "Inside CASE4 Condition:  else  part : partialPacketFirstPartLength Check 0x70) prevOffset = offset; prevOffset: " + prevOffset);
                            //Log.d("Mocxa", "Inside CASE4 Condition:  else  part : partialPacketFirstPartLength Check 0x70) offset += partialPacketLastPartLength; offset: " + offset);
                            //Log.d("Mocxa", "Inside CASE4 Condition:  else  part : partialPacketFirstPartLength Check 0x70)lengthCovered += partialPacketLastPartLength;; lengthCovered: " + lengthCovered);
                            //received a valid packet with less number of bytes in the packet
                            if (offset > bufferLength) {
                                //  Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1869 else  part : partialPacketFirstPartLength Check 0x70) Inside offset - 1 > bufferLength");
                                offset = bufferLength;
                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1871 else  part : partialPacketFirstPartLength Check 0x70) Inside offset - 1 > bufferLength; offset = bufferLength; offset: " + offset);
                            }
                            if (partialPacketLastPartLength > bufferLength) {
                                partialPacketLastPartLength = bufferLength;
                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 2013 else  part : partialPacketFirstPartLength Check 0x70) Inside opartialPacketLastPartLength > bufferLength;  partialPacketLastPartLength = bufferLength; partialPacketLastPartLength: " + partialPacketLastPartLength);
                            }
                                            /* ToDo
                                             java.lang.ArrayIndexOutOfBoundsException: length=1493; index=-1
        at com.mocxa.eeg.LiveGraphActivity$ProcessingThread.run(LiveGraphActivity.java:3027) */

                            if (databuf[offset - 1] != (byte) 0x0d) {
                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1873 else  part : partialPacketFirstPartLength Check 0x70) Inside (databuf[offset - 1] != (byte) 0x0d); offset: " + offset);
                                newOffset = GetEOPOffset(databuf, offset - 2);
                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1876 else  part : partialPacketFirstPartLength Check 0x70) Inside (databuf[offset - 1] != (byte) 0x0d);  newOffset = GetEOPOffset(databuf, offset - 2); newOffset: " + newOffset);
                                if (newOffset == -1) {
                                    offset = prevOffset;
                                    //  Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1879 else  part : partialPacketFirstPartLength Check 0x70) Inside (databuf[offset - 1] != (byte) 0x0d);  newOffset == -1; offset = prevOffset; offset: " + offset);
                                    for (int i = partialPacketFirstPartLength; i < (partialPacketFirstPartLength + partialPacketLastPartLength) - 2; i++) {

                                        eegSpecialPacket[i] = (byte) 0x20;
                                    }
                                    eegSpecialPacket[(partialPacketFirstPartLength + partialPacketLastPartLength) - 1] = (byte) 0x0d;
                                    int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                    eegGraphPacketList.add(localEEGData);
                                    //setting the EEG Special packet Status to true once the EEG Speecial packet processing completed
                                    eegSpecialpacketStatus = true;

                                    //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1886 else  part : partialPacketFirstPartLength Check 0x70) Ieeg special packet0x20 and 0d added eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));


                                } else {
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1891 else  part : newOffset == -1 packetSize: " + packetSize);

                                    packetSize = packetSize - (offset - newOffset);
                                    offset = newOffset;

                                    System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                    int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, partialPacketFirstPartLength + partialPacketLastPartLength, channel_nos);
                                    eegGraphPacketList.add(localEEGData);
                                    //setting the EEG Special packet Status to true once the EEG Speecial packet processing completed
                                    eegSpecialpacketStatus = true;

                                    //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1897 else  part : newOffset == -1 packetSize = packetSize - (offset - newOffset); packetSize: " + packetSize);
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1898 else  part : newOffset == -1  offset = newOffset;  offset" + offset);
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1898 else  part : newOffset == -1  partialPacketLastPartLength " + partialPacketLastPartLength);
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1898 else  part : newOffset == -1  eegSpecialPacket " + Arrays.toString(eegSpecialPacket));
                                }

                            } else {
                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1904 else  part : databuf[offset - 1] != (byte) 0x0d  offset" + offset);
                                offset = partialPacketLastPartLength;
                                System.arraycopy(databuf, prevOffset, eegSpecialPacket, partialPacketFirstPartLength, partialPacketLastPartLength);
                                int[] localEEGData = Decrypt_EEG(eegSpecialPacket, 0, (partialPacketFirstPartLength + partialPacketLastPartLength), channel_nos);
                                eegGraphPacketList.add(localEEGData);
                                //setting the EEG Special packet Status to true once the EEG Speecial packet processing completed
                                eegSpecialpacketStatus = true;

                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1904 else  part : databuf[offset - 1] != (byte) 0x0d    offset = partialPacketLastPartLength; offset" + offset);
                                //Log.d("Mocxa", "Inside CASE4 Condition:  LINE 1904 else  part : databuf[offset - 1] != (byte) 0x0d    EEg Special packet copied eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));

                            }

                        }

                    } else if (eegSpecialPacket[1] == (byte) 0x68) {
                        //Log.d("Mocxa", "Inside CASE4 Condition: Line : 1916 eegSpecialPacket [offset + 1] Check 0x68: packetType " + packetType);
                        partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                        byte status = eegSpecialPacket[3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB
                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            // Log.d(TAG, "Inside CASE4 Condition: Line : 1916 eegSpecialPacket [offset + 1] Check 0x68 DB Writing the event status" + status);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;

                        //Log.d("Mocxa", "Inside CASE4 eegSpecialPacket [offset + 1 Check 0x68: partialPacketFirstPartLength " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE4 partialPacketLastPartLength Check 0x68:  partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength; partialPacketLastPartLength: " + partialPacketLastPartLength);
                        //Log.d("Mocxa", "Inside CASE4 partialPacketFirstPartLength Check0x68 :status: " + status);

                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x68 :offset += T4_HEARTBEAT_PACKET_SIZE; offset " + offset);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x68 :lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered " + lengthCovered);


                    } else if (eegSpecialPacket[1] == (byte) 0x61) {
                        //Log.d("Mocxa", "Inside CASE4 Condition: Line : 1937 eegSpecialPacket [1] Check 0x61: packetType " + packetType);
                        partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                        byte status = eegSpecialPacket[3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB

                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            // Log.d(TAG, "Inside CASE4 Condition: Line : 1937 eegSpecialPacket [1] Check 0x61 DBWriting the event status: " + status);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d("Mocxa", "Inside CASE4 Line 1948Condition Check 0x61:packetType " + packetType);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check 0x61:partialPacketFirstPartLength:  " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check 0x61:partialPacketLastPartLength: " + partialPacketLastPartLength);
                        //Log.d("Mocxa", "Inside CASE4 Condition:h Check0x61 :status: " + status);

                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x61 : offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x61 :  lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);

                    } else if (eegSpecialPacket[1] == (byte) 0x6E) {
                        //Log.d("Mocxa", "Inside CASE4 Condition: Line : 1957 eegSpecialPacket [1] Check 0x6E: packetType " + packetType);
                        partialPacketLastPartLength = T4_HEARTBEAT_PACKET_SIZE - partialPacketFirstPartLength;
                        byte status = eegSpecialPacket[3];
                        byte isEvent = (byte) (status & (byte) 0x01);
                        if (isEvent == (byte) 0x01) {
                            // Log event in the DB

                             /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                            patientEvents.add("Event");
                            // Log.d(TAG, "inside CASE4 Condition: Line : 1964 eegSpecialPacket [1] Check 0x6E dWriting the event status: " + status);
                        }
                        offset += T4_HEARTBEAT_PACKET_SIZE;
                        lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                        //Log.d("Mocxa", "Inside CASE4 Line 1968 Condition: Check 0x6E:packetType " + packetType);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check 0x6E:partialPacketFirstPartLength: " + partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check 0x6E:partialPacketLastPartLength: " + partialPacketLastPartLength);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x6E :status: " + status);

                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x6E : offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                        //Log.d("Mocxa", "Inside CASE4 Condition: Check0x6E : lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);
                        /*
                        TODO uncomment
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        Date date = new Date();
                        System.out.println(formatter.format(date));
                        String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";
                        fileWriteHandler.logUpdate(logMsg);*/
                        nackEvents.add(new ModelPacketEventNack(eegSpecialPacket[2],status));

                    } else {

                        offset++;
                        //Log.d(TAG, "Inside CASE4 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                    }


                }
                //Log.d("Mocxa", "Inside CASE4 End of if offset==0 condition");
            }

            // Remaining is the condition of 0x1C && !0x0D
            do {

                // Check for partial packet first to avoid index out of bound error
                // Log.d("Mocxa", "Inside CASE4 Condition do while loop");

                //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset is non zero do loop; offset: " + offset);
                //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset is non zero do loop; bufferLength: " + bufferLength);
                if (offset == bufferLength) break;

                if (offset < bufferLength) {
                    if (databuf[offset] != (byte) 0x1c) {

                        //Log.d("Mocxa", "Inside CASE4 Condition: (offset < bufferLength) databuf[offset] != (byte) 0x1C");
                        //Log.d("Mocxa", "Inside CASE4 Condition:  (offset < bufferLength) databuf[offset] != (byte) 0x1C;  offset: " + offset);
                        //Log.d("Mocxa", "Inside CASE4 Condition:  (offset < bufferLength) databuf[offset] != (byte) 0x1C;  bufferLength: " + bufferLength);
                        offset = GetSOPOffset(databuf, offset + 1, bufferLength);
                        //Log.d("Mocxa", "Inside CASE4 Condition:  Line 1999(offset < bufferLength) databuf[offset] != (byte) 0x1C; offset = GetSOPOffset(databuf, offset + 1, bufferLength);  offset: " + offset);
                        if (offset == -1) {
                            offset = bufferLength;
                            //Log.d("Mocxa", "Inside CASE4 Condition:  Line 2003(offset < bufferLength) databuf[offset] != (byte) 0x1C; Inside (offset== -1)check offset = bufferLength  offset: " + offset);
                        }
                    }
                }
                if (offset == bufferLength) break;

                if (databuf[offset] == (byte) 0x1c) {
                    //Log.d("Mocxa", "Inside CASE4 Condition: databuf[offset] == (byte) 0x1c offset is non zero do loop --offset; " + offset);
                    int bytesRemaining = bufferLength - offset;
                    //Log.d("Mocxa", "Inside CASE4 Condition: offset is non zero do loop --offset; " + offset);
                    //Log.d("Mocxa", "Inside CASE4 Condition: offset is non zero do loop --bufferLength; " + bufferLength);
                    //Log.d("Mocxa", "Inside CASE4 Line 2014Condition: offset is non zero do loop --bytesRemaining; " + bytesRemaining);

                    if (bytesRemaining <= 3) {
                        //Log.d("Mocxa", "Inside CASE4 Line 2017 Condition: bytesRemaining <= 3 condition;-bytesRemaining; " + bytesRemaining);
                        partialPacketFirstPartLength = bytesRemaining;
                        //Log.d("Mocxa", "Inside CASE4 Line 2019 Condition: bytesRemaining <= 3 condition;-partialPacketFirstPartLength = bytesRemaining;  partialPacketFirstPartLength " + partialPacketFirstPartLength);
                        System.arraycopy(databuf, offset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                        //Log.d("Mocxa", "Inside CASE4 Line 2021 Condition: bytesRemaining <= 3 condition;-eegSpecialPacket:  " + Arrays.toString(eegSpecialPacket));
                        //setting the EEG Special packet Status to false once the EEG Speecial packet processing not completed
                        eegSpecialpacketStatus = false;

                        offset += partialPacketFirstPartLength;
                        lengthCovered += partialPacketFirstPartLength;
                        //Log.d("Mocxa", "Inside CASE4 Condition: offset is non zero do loop  offset += partialPacketFirstPartLength; --offset; " + offset);
                        //Log.d("Mocxa", "Inside CASE4 Condition: offset is non zero do loop --bufferLength; " + bufferLength);
                        // Log.d("Mocxa", "Inside CASE4 Condition: offset is non zero do loop --partialPacketFirstPartLength; " +partialPacketFirstPartLength );
                        // Log.d("Mocxa", "Inside CASE4 Condition: offset is non zero do loop --eegSpecialPacket; " +Arrays.toString(eegSpecialPacket) );


                    } else {
                        packetSize = (databuf[offset + 3] - (byte) 0x20) * 3 + 6;
                        //Log.d("Mocxa", "Inside CASE4 Condition: Line 2033 else part of bytesRemaining <= 3  offset: " + offset);
                        //Log.d("Mocxa", "Inside CASE4 Condition: else part of bytesRemaining <= 3 Calculated packetSize:  " + packetSize);
                        if (offset + packetSize > bufferLength) {
                            if (offset + 4 < bufferLength && databuf[offset + 4] == T4_RES_TYPE) {
                                partialPacketFirstPartLength = bufferLength - offset;
                                //Log.d("Mocxa", "Inside CASE4 Condition: else part of bytesRemaining <= 3 inside (offset + packetSize > bufferLength) condition bufferLength:   " + bufferLength);
                                //Log.d("Mocxa", "Inside CASE4 Condition: else part of bytesRemaining <= 3 inside (offset + packetSize > bufferLength) condition offset:   " + offset);
                                //Log.d("Mocxa", "Inside CASE4 Condition: else part of bytesRemaining <= 3 inside (offset + packetSize > bufferLength) condition partialPacketFirstPartLength = bufferLength - offset;  partialPacketFirstPartLength:  " + partialPacketFirstPartLength);
                                System.arraycopy(databuf, offset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                                //setting the EEG Special packet Status to false once the EEG Speecial packet processing not completed
                                eegSpecialpacketStatus = false;

                                offset += partialPacketFirstPartLength;
                                lengthCovered += partialPacketFirstPartLength;

                                //Log.d("Mocxa", "Inside CASE4 Condition:Line 2044 offset is non zero do loop  -else part of bytesRemaining <= 3 inside (offset + packetSize > bufferLength)  offset += partialPacketFirstPartLength; offset:" + offset);
                                //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset is non zero do loop -else part of bytesRemaining <= 3 inside (offset + packetSize > bufferLength)lengthCovered += partialPacketFirstPartLength; lengthCovered:" + lengthCovered);
                                //Log.d("Mocxa", "Inside CASE4 Condition: case 4- offset is non zero do loop-else part of bytesRemaining <= 3 inside (offset + packetSize > bufferLength); eegSpecialPacket: " + Arrays.toString(eegSpecialPacket));
                            } else {
                                System.arraycopy(eegZeroPacket, 0, eegSpecialPacket, 0, T4_packet_length);
                                offset++;

                            }

                        } else if (databuf[offset + 1] == (byte) 0x70) {

                            //Log.d("Mocxa", "Inside CASE4 Condition: Line 2051 offset is non zero do loop --else part offset + packetSize > bufferLength");
                            //Log.d("Mocxa", "Inside CASE4 Condition:offset is non zero do loop databuf[offset + 1] == (byte) 0x70");
                            if (databuf[offset + 4] == T4_RES_TYPE) {
                                //Log.d("Mocxa", "Inside CASE4 Condition: databuf[offset + 4] == T4_RES_TYPE Valid packet");
                                //Log.d("Mocxa", "Inside CASE4 databuf[offset + 4] == T4_RES_TYPE Valid packet offset: " + offset);

                                int prevOffset = offset;
                                offset += packetSize;
                                lengthCovered += packetSize;
                                int newOffset = offset;

                                //Log.d("Mocxa", "Inside CASE4 Condition:LINE 2062 offset is non zero do loop databuf[offset + 1] == (byte) 0x70  prevOffset = offset; prevOffset: " + prevOffset);
                                //Log.d("Mocxa", "Inside CASE4 Condition offset is non zero do loop databuf[offset + 1] == (byte) 0x70   offset += packetSize; offset: " + offset);
                                //Log.d("Mocxa", "Inside CASE4 Condition offset is non zero do loop databuf[offset + 1] == (byte) 0x70  lengthCovered += packetSize; lengthCovered: " + lengthCovered);
                                //Log.d("Mocxa", "Inside CASE4 Condition offset is non zero do loop databuf[offset + 1] == (byte) 0x70 newOffset = offset; newOffset: " + lengthCovered);


                                //received a valid packet with less number of bytes in the packet
                                if (offset > bufferLength) {
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  offset is non zero do loop inside offset - 1 > bufferLength condition offset: " + offset);
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  offset is non zero do loop inside offset - 1 > bufferLength condition bufferLength: " + bufferLength);
                                    offset = bufferLength;
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  offset is non zero do loop inside offset - 1 > bufferLength condition  offset = bufferLength;:  offset: " + offset);
                                }
                                if (databuf[offset - 1] != (byte) 0x0d) {
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  offset is non zero do loop inside  databuf[offset - 1] != (byte) 0x0d:  offset: " + offset);
                                    newOffset = GetEOPOffset(databuf, offset - 2);
                                    //Log.d("Mocxa", "Inside CASE4 Condition:  offset is non zero do loop inside  databuf[offset - 1] != (byte) 0x0d:   newOffset = GetEOPOffset(databuf, offset - 2); newOffset: " + newOffset);
                                    if (newOffset == -1) {
                                        offset++;
                                        //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                        //Log.d("Mocxa", "Inside CASE4 Condition: Line 2090 This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                        //Log.d("Mocxa", "Inside CASE4 Condition: Line 2091 offset is non zero do loop --else part packetSize; offset Rare case offset: " + offset);
                                        //Log.d("Mocxa", "Inside CASE4 Condition: Line 2092offset is non zero do loop --else part packetSize; offset Rare packetSize: " + packetSize);

                                    } else {
                                        if (newOffset == prevOffset) {
                                            offset++;
                                            //This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet.
                                            //Log.d("Mocxa", "Inside CASE4 Condition: Line 2098 Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize This is encounered  when a packet with 1C and no 0d  this is the FIRST PACKET. We discard this packet. offset: " + offset);
                                        } else {
                                            int validEOPposition = (offset - 1) - newOffset;
                                            if (validEOPposition >= packetSize) {
                                                databuf[offset - 1] = (byte) 0x0d;
                                                //EEG_Data = Decrypt_EEG(databuf, prevOffset, packetSize);
                                                //Log.d("Mocxa", "Inside CASE4 Condition: Line 2104 Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize");
                                                //Log.d("Mocxa", "Inside CASE4 Condition:Checking for the new Packet Size and New Offset When 0D Not found validEOPposition >= packetSize validEOPposition  " + validEOPposition);
                                                //Log.d("Mocxa", "Inside CASE4 Condition:Checking for the new Packet Size and New Offset When 0D Not found  offset validEOPposition offset: " + offset);
                                                //Log.d("Mocxa", "Inside CASE4 Condition: Checking for the new Packet Size and New Offset When 0D Not found packetSize validEOPposition packetSize: " + packetSize);
                                            } else {
                                                packetSize = packetSize - (offset - newOffset);
                                                offset = newOffset;
                                                //    EEG_Data = Decrypt_EEG(databuf, prevOffset, packetSize);
                                                //Log.d("Mocxa", "Inside CASE4 Condition:Line 2112 Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize");
                                                //Log.d("Mocxa", "Inside CASE4 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of validEOPposition >= packetSize  offset: " + offset);
                                                //Log.d("Mocxa", "Inside CASE4 Condition: Checking for the new Packet Size and New Offset When 0D Not found else part of  validEOPposition >= packetSize packetSize: " + packetSize);
                                            }
                                        }
                                    }
                                } else {
                                    // Log.d("Mocxa", "Inside CASE1 Condition: Else part of databuf[offset - 1] != (byte) 0x0d ");
                                    if (prevOffset + packetSize >= bufferLength) {
                                        partialPacketFirstPartLength = (bufferLength - prevOffset);
                                        System.arraycopy(databuf, prevOffset, eegSpecialPacket, 0, partialPacketFirstPartLength);
                                        eegSpecialpacketStatus = false;

                                        offset = bufferLength;
                                        lengthCovered = bufferLength;
                                    } else {
                                        int[] localEEGData = Decrypt_EEG(databuf, prevOffset, packetSize, channel_nos);
                                        eegGraphPacketList.add(localEEGData);

                                    }

                                }
                                                /*{
                                                int[] localEEGData  = Decrypt_EEG(databuf, prevOffset, packetSize);
                                                EEG_Data = localEEGData;
                                                eegGraphPacketList.add(localEEGData);
                                                //Log.d("Mocxa", "Inside CASE4 Condition:else part of databuf[offset - 1] != (byte) 0x0d");
                                            }*/
                            } else {
                                //hack to get away from 1c check
                                offset++;
                                //Log.d("Mocxa", "Inside CASE4 Condition:else part of databuf[offset + 4 ] == T4_RES_TYPE  offset++;  offset: " + offset);
                            }

                        } else if (databuf[offset + 1] == (byte) 0x68 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                            //Log.d("Mocxa", "Inside CASE4 Condition LINE 2121 Inside (byte) 0x68 heartbeat ack ");
                            byte status1 = databuf[offset + 2];
                            //Log.d("Mocxa", "CASE 4 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                            int chkAqg = status1 >> 1 & 1;
                                           /* if (chkAqg == 0) {
                                                //Log.d("Mocxa", "CASE 4 Acquisition done, chkAqg: " + chkAqg);
                                                onGoingsStopped = true;
                                            }*/
                            byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //   Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x68 heartbeat ack dWriting the event status2" + status2);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;
                            //Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x68 heartbeat ack offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                            //Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x68 heartbeat lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);

                        } else if (databuf[offset + 1] == (byte) 0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                            // This is a device transmit ACK packet. Check for event
                            //Log.d("Mocxa", "Inside CASE4 Condition LINE 2137 Inside (byte) 0x61 ACK packet");
                            byte status1 = databuf[offset + 2];
                            //Log.d("Mocxa", "CASE 4 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                            int chkAqg = status1 >> 1 & 1;
                                          /*  if (chkAqg == 0) {
                                                //Log.d("Mocxa", "CASE 4 Acquisition done, chkAqg: " + chkAqg);
                                                onGoingsStopped = true;
                                            }*/
                            byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                //   Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x61 ACK packet dWriting the event status2" + status2);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;

                            //Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x61 ack offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                            //Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x61 ack lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);


                        } else if (databuf[offset + 1] == (byte) 0x6e && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                            // This is a device transmit NACK packet. Check for event
                            //Log.d("Mocxa", "Inside CASE4 Condition LINE 2154 Inside (byte) 0x6E NACK packet");
                            byte status1 = databuf[offset + 2];
                            //Log.d("Mocxa", "CASE 4 Checking the Event Status bit in heartbeatFor AQG false " + status1);
                            int chkAqg = status1 >> 1 & 1;
                                          /*  if (chkAqg == 0) {
                                                //Log.d("Mocxa", "CASE 4 Acquisition done, chkAqg: " + chkAqg);
                                                onGoingsStopped = true;
                                            }*/
                            byte status2 = databuf[offset + 3];
                            byte isEvent = (byte) (status2 & (byte) 0x01);
                            if (isEvent == (byte) 0x01) {
                                // Log event in the DB
                                 /*TODO uncomment
                               db.updateevent(patient_id, patient_tid, "Event");*/
                                patientEvents.add("Event");
                                // Log.d(TAG, "dWriting the event" + databuf);
                            }
                            offset += T4_HEARTBEAT_PACKET_SIZE;
                            lengthCovered += T4_HEARTBEAT_PACKET_SIZE;

                            //Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x6E Nack offset += T4_HEARTBEAT_PACKET_SIZE; offset: " + offset);
                            //Log.d(TAG, "Inside CASE4 Condition Inside (byte) 0x6E NACK lengthCovered += T4_HEARTBEAT_PACKET_SIZE; lengthCovered: " + lengthCovered);
                            /*
                            TODO uncomment
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            Date date = new Date();
                            System.out.println(formatter.format(date));
                            String logMsg = formatter.format(date) + " Setup Completed: " + setupSent + "  :NACK Received \n";
                            fileWriteHandler.logUpdate(logMsg);*/
                            nackEvents.add(new ModelPacketEventNack(status1, status2));
                        } else {
                            //packet is entirely wrong We are skipping the packet
                            offset++;
                            //Log.d(TAG, "Inside CASE4 Condition else part of all the conditions, 70, 61,6e and 68  offset: " + offset);
                        }
                    }
                }
                if (offset == bufferLength) {
                    endOfPacket = true;
                    //Log.d(TAG, "Inside CASE4 Condition Inside offset == bufferLength condition; offset: " + offset);
                    //Log.d(TAG, "Inside CASE4 Condition Inside offset == bufferLength condition; bufferLength: " + bufferLength);
                    //Log.d(TAG, "Inside CASE4 Condition Inside offset == bufferLength condition; endOfPacket: " + endOfPacket);
                }

            } while (endOfPacket == false); //end of while

        } // end of case 4

        //Log.d("Mocxa", "end of all cases");

       /* if (pressStop == true) {
            int offset = bufferLength - 6;
            //Log.d("Mocxa", "Inside End of all cases Condition LINE 2154 Inside (byte) 0x6E ACK packet   " +offset);
            if (databuf[offset] == (byte) 0x1c) {
                if (databuf[offset + 1] == (byte) 0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {
                    // This is a device transmit ACK packet. Check for Stop transmit and acquire
                    //Log.d("Mocxa", "Inside End of all cases Condition LINE 2154 Inside (byte) 0x6E ACK packet");
                    byte status1 = databuf[offset + 2];
                    //Log.d("Mocxa", "End of all cases Checking the Event Status bit in heartbeatFor AQG false " + status1);
                    int chkAqg = status1 >> 1 & 1;
                    if (chkAqg == 0) {
                        //Log.d("Mocxa", "End of all cases Acquisition done, chkAqg: " + chkAqg);
                        onGoingsStopped = true;
                    }
                }

            }

            offset = 0;
            if (databuf[offset] == (byte) 0x1c && databuf[offset + 1] == (byte) 0x61 && databuf[offset + T4_HEARTBEAT_PACKET_SIZE - 1] == (byte) 0x0d) {

                // This is a device transmit ACK packet. Check for Stop transmit and acquire
                //Log.d("Mocxa", "Inside End of all cases Condition LINE 2154 Inside (byte) 0x6E ACK packet");
                byte status1 = databuf[offset + 2];
                //Log.d("Mocxa", "End of all cases  Checking the Event Status bit in heartbeatFor AQG false " + status1);
                int chkAqg = status1 >> 1 & 1;
                if (chkAqg == 0) {
                    //Log.d("Mocxa", "End of all cases  Acquisition done, chkAqg: " + chkAqg);
                    onGoingsStopped = true;
                }


            }
        }*/

        //  }
        //---------------------------------------------
        /*
        TODO move to other place
        if (EEG_Data.length >= 8) {
            // inserting to file

                          *//*  new insertReceivedData_async()
                                    .execute(EEG_Data);*//*
            if (pipactive) {
                plotData = false;
                Startplot = true;
                //fileupdate(EEG_Data);
                                *//*new insertReceivedData_async()
                                        .execute(EEG_Data);*//*
                if (firsttimeStart) {
                    db.updateevent(patient_id, patient_tid, "Start-Recording");

                    firsttimeStart = false;
                }
            }
                          *//* if (LiveGraphActivity.Setup == false) {
                                if (!Startplot) {
                                    startPlot(EEG_Data_Display);
                                    Startplot = true;
                                }
                            }*//*
            // chatMessages.add(connectingDevice.getName() + ":  " + readBuf2);
            //ToDO remove in future chatAdapter.notifyDataSetChanged();

        }*/
        // Log.d("Mocxa", "just before break");
        Long endTimeNano = System.nanoTime();


        modelPacket.processingTimePeriod = (endTimeNano - startTimeNano);

        modelPacket.eegGraphPacketList = eegGraphPacketList;
        modelPacket.patientEvents = patientEvents;
        return modelPacket;


    }
}
