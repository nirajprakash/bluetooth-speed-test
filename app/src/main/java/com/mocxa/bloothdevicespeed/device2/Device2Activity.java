package com.mocxa.bloothdevicespeed.device2;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mocxa.bloothdevicespeed.BluetoothConstants;
import com.mocxa.bloothdevicespeed.common.ReceiveThread;
import com.mocxa.bloothdevicespeed.databinding.ActivityDevice2Binding;
import com.mocxa.bloothdevicespeed.device.DeviceActivity;
import com.mocxa.bloothdevicespeed.tools.UtilLogger;
import com.mocxa.bloothdevicespeed.tools.livedata.LiveDataObserver;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Device2Activity extends AppCompatActivity {
    private UtilLogger log = UtilLogger.with(this);
    private String mCurrentMessage = null;
    private int mReadBytes = 0;
    private int mSelectedDeviceIndex = -1;
    BluetoothAdapter mBluetoothAdapter = null;

    BluetoothDevice2Service mBluetoothService = null;

    private boolean isAlreadySearched = false;

    ActivityResultLauncher mActivityResultBluetoothEnable =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//                info{"mActivityResultSignin: ${result.data}"}
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBluetoothService();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d("MainActivity", "BT not enabled");

                    Toast.makeText(
                            this,
                            "bt_not_enabled_leaving",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            });

    HandlerThread mHandlerThread = new HandlerThread("MySuperAwsomeHandlerThread");

    /**
     * Newly discovered devices
     */
    private List<BluetoothDevice> mNewDevicesArrayAdapter = new ArrayList<>();

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            Log.i("MainActivity:", "BroadcastReceiver: " +
                    action);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(
                            device
                    );
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                if (isAlreadySearched) {

                    isAlreadySearched = false;
                } else {
                    if (mNewDevicesArrayAdapter.size() == 0) {
                        Toast.makeText(
                                Device2Activity.this,
                                "no result found",
                                Toast.LENGTH_SHORT
                        ).show();

                        vBinding.device2SearchLog.setText("No device found");
                    } else {
                        vBinding.device2SearchLog.setText("Search compete");
                        showDialogResolutionSelect();
                    }
                    if (mBluetoothAdapter != null) {
                        isAlreadySearched = true;
                        mBluetoothAdapter.cancelDiscovery();
                    }
                }


            }
        }
    };


    private ActivityDevice2Binding vBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vBinding = ActivityDevice2Binding.inflate(getLayoutInflater());
        View view = vBinding.getRoot();
        setContentView(view);
        vBinding.device2Search.setOnClickListener(v -> {
            vBinding.device2SearchLog.setText("Searching");
            bluetoothSearchSetup();

        });

        vBinding.device2SearchCancel.setOnClickListener(v -> {
            Log.i("MainActivity", "blSearchCancel: ");
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        });

        vBinding.device2SendReceive.setOnClickListener(v -> {
            vBinding.device2SendReceiveFreqLog.setText("..");
            mBluetoothService.setupSendReceive();

        });

        vBinding.device2SendSetup.setOnClickListener(v -> {
            /*vBinding.device2SendFreqLog.setText("..");
            mBluetoothService.setupSender();*/
            mBluetoothService.sendSetup();
        });

        vBinding.device2StartEeg.setOnClickListener(v -> {
            mBluetoothService.startEEG();
        });

        vBinding.device2Stop.setOnClickListener(v -> {
            mBluetoothService.stopEEG();
/*            mBluetoothService.stopChat();
            mReadBytes = 0;
            mCurrentMessage = null;*/
        });

        vBinding.device2Counter.setOnClickListener(v -> {
            String logStr = mBluetoothService.logService();

            String currentMessage = mCurrentMessage;
            int readBytes = mReadBytes;
            if (currentMessage != null) {
                Log.i("mainActivity: ", "mCurrentMessage 1");
                logStr = logStr +
                        "  \n readBytesCounter: " +
                        readBytes +
                        " \n last message: " +
                        currentMessage;

            }
//            Log.i("mainActivity: ", logStr);

            log.i(logStr);
            mBluetoothService.resetCounterLog();
            vBinding.device2CounterLog.setText(logStr);

        });

        setupBluetooth();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i("mainActivity: ", "onStart 1");
        if (mBluetoothAdapter == null) {
            return;
        }
        Log.i("mainActivity: ", "onStart 2");
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivityResultBluetoothEnable.launch(enableIntent);

            // Otherwise, setup the chat session
        } else if (mBluetoothService == null) {
            Log.i("mainActivity: ", "onStart 3");
            setupBluetoothService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothConstants.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }

        try {
            // Unregister broadcast listeners
            this.unregisterReceiver(mReceiver);
        } catch (Exception e) {

        }

    }

    private void setupBluetooth() {

        Log.i("MainActivity: ", "setupBluetooth");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


    }

    private void setupBluetoothService() {
        Log.i("mainActivity: ", "onStart 4");

        if (mBluetoothAdapter == null) return;
        mHandlerThread.start();
        Handler readerHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
            /*    Log.d(
                        "MainActivity",
                        "handleMessage " + msg.what + " in " +
                                (msg.obj != null)
                );*/
                if (msg.what == Device2ReceiveThread.MESSAGE_READ) {
                    /*Log.d(
                            "MainActivity",
                            "handleMessage in"
                    );*/
                    Object objData = msg.obj;

                    if (objData != null) {
//                        Log.i("mainActivity: ", "mCurrentMessage 2");

                      /*  TODO uncomment byte[] readBuf = (byte[]) objData;
                        // construct a string from the valid bytes in the buffer
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        mCurrentMessage = readMessage;*/
                        mReadBytes += msg.arg1;
                        ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) msg.obj;
                        byte[] readBuf = byteArrayOutputStream.toByteArray();
                        log.i( "read Message 1:");

                        //lastMessage = new String(readBuf, 0, msg.arg1);

                        if (readBuf != null && mBluetoothService != null) {
                            log.i( "read Message 2:");

//                            Log.d("Live Graph", "");
                            mBluetoothService.onBytes(readBuf);
//                            packetQueue.add(readBuf);
                        }

                    }
                }

            /* Log.d(
                    "MainActivity",
                    "handleMessage " + msg.what.toString() + " in " + Thread.currentThread()
                )*/
            }

        };
        mBluetoothService = new BluetoothDevice2Service(mBluetoothAdapter, readerHandler);

        mBluetoothService.mEventErrorMessage.observe(this,
                new LiveDataObserver<String>(data -> {
                    Toast.makeText(
                            Device2Activity.this,
                            data,
                            Toast.LENGTH_SHORT
                    ).show();
                }));


        mBluetoothService.mEventErrorMessageMediator.observe(this, stringLiveDataEvent -> {
        });

        mBluetoothService.mEventConnectedMediator.observe(this, bluetoothSocketLiveDataEvent -> {
        });

        mBluetoothService.mEventConnected.observe(this, new LiveDataObserver<BluetoothSocket>(data -> {
            Toast.makeText(
                    Device2Activity.this,
                    "Connected",
                    Toast.LENGTH_SHORT
            ).show();
            vBinding.device2ConnectLog.setText("Connected");
        }));

        mBluetoothService.mEventNackError.observe(this, new LiveDataObserver<String>(
                data -> {

                    if(data!=null){
                        showDialogNackError(data);
                    }

                }
        ));

        mBluetoothService.mEventMessage.observe(this, new LiveDataObserver<String>(
                data -> {

                    if(data!=null){
                        Toast.makeText(
                                Device2Activity.this, data,
                                Toast.LENGTH_SHORT
                        ).show();

                    }

                }
        ));
    }


    /* ***************************************************************************
     *                          List
     */

    public void showDialogResolutionSelect() {

        new MaterialAlertDialogBuilder(this).setSingleChoiceItems(
                mNewDevicesArrayAdapter.stream().map(bluetoothDevice ->
                        bluetoothDevice.getName() +
                                "\n"
                                +
                                bluetoothDevice.getAddress()).collect(Collectors.toList())
                        .toArray(new String[mNewDevicesArrayAdapter.size()])
                ,
                -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(
                                "ActivitySetting: ",
                                "showDialogResolutionSelect: " + Integer.valueOf(which)
                        );
                        mSelectedDeviceIndex = which;
                    }
                }
        ).setPositiveButton("Submit",
                (dialog, which) -> {
                    Log.d(
                            "ActivitySetting: ",
                            "showDialogResolutionSelect submit: " + Integer.valueOf(which)
                    );

                    if (mSelectedDeviceIndex >= 0) {
                        startIntentPairing(mSelectedDeviceIndex);
                    }
                })
                .show();


    }

    private void startIntentPairing(int index) {
        if (mNewDevicesArrayAdapter.size() > index) {
            BluetoothDevice device = mNewDevicesArrayAdapter.get(index);
            connectDevice(device);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        if (mBluetoothAdapter == null) {
            return;
        }
        log.i("connect Device: " + device);
        BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(device.getAddress());
        // Attempt to connect to the device
        // Attempt to connect to the device
        if (btDevice != null) {
            mBluetoothService.connect(btDevice);
        }
    }

    /* *******************************************************************************
     *                                Bluetooth search
     */

    public void bluetoothSearchSetup() {
        mNewDevicesArrayAdapter.clear();


        // Register for broadcasts when a device is discovered
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
//        filter = IntentFilter(BluetoothAdapter.ACTION_)
//        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);


        // Get a set of currently paired devices

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices != null) {

            mNewDevicesArrayAdapter.addAll(pairedDevices);

            vBinding.device2PairedLog.setText("Paired Device: " + pairedDevices.size());

        }

        doDiscovery();

    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (mBluetoothAdapter == null) {
            return;
        }
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            isAlreadySearched = true;
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    /* ***********************************************************************************
     *
     */

    public void showDialogNackError(String message) {

        new MaterialAlertDialogBuilder(this).
                setMessage(message)
                .setPositiveButton("Retry",
                        (dialog, which) -> {
                            log.d("showDialogNackError retry: ");
                            mBluetoothService.retrySend();
                        })
                .setNegativeButton("Stop",
                        (dialog, which) -> {
                            log.d("showDialogNackError stop ");
                            mBluetoothService.stopEEG();
                            new Handler().postDelayed(() -> {
                                mBluetoothService.stop();
                                finish();
                            }, 300);

                        })
                .show();


    }
}