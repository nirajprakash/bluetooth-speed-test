package com.mocxa.bloothdevicespeed;

import java.util.UUID;

/**
 * Created by Niraj on 01-01-2022.
 */
public class BluetoothConstants {

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

//    public static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
//    public static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    public static final int STATE_NONE = 0; // we're doing nothing

    public static final int STATE_LISTEN = 1; // now listening for incoming connections

    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection

    public static final int STATE_CONNECTED = 3; // now connected to a remote device

}
