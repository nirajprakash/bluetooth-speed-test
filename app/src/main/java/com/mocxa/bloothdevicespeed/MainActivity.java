package com.mocxa.bloothdevicespeed;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mocxa.bloothdevicespeed.databinding.ActivityMainBinding;
import com.mocxa.bloothdevicespeed.device.DeviceActivity;
import com.mocxa.bloothdevicespeed.device2.Device2Activity;
import com.mocxa.bloothdevicespeed.mobile.BluetoothMobileService;
import com.mocxa.bloothdevicespeed.mobile.MobileActivity;
import com.vmadalin.easypermissions.EasyPermissions;
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted;
import com.vmadalin.easypermissions.dialogs.SettingsDialog;

import java.io.DataInputStream;
import java.util.List;

import javax.microedition.io.Connector;

public class MainActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks {

    static final int  REQUEST_CODE_PERMISSION = 10212;


    private ActivityMainBinding vBinding;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = vBinding.getRoot();
        setContentView(view);

        vBinding.mainMobileDevice.setOnClickListener(v -> {
            startActivityMobileDevice();
        });

        vBinding.mainMobileMobile.setOnClickListener(v -> {
            startActivityMobile();
        });
        vBinding.mainMobileDevice2.setOnClickListener(v -> {
            startActivityMobileDevice2();
        });
//        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            startProcess();
        }else{
            startProcessBelowS();
        }
    }

    private void startActivityMobile() {
        startActivity(new Intent(this, MobileActivity.class));

    }



    private void startActivityMobileDevice() {
        startActivity(new Intent(this, DeviceActivity.class));

    }
    private void startActivityMobileDevice2() {
        startActivity(new Intent(this, Device2Activity.class));

    }

    @Override
    public void onPermissionsDenied(int i, @NonNull List<String> list) {
        Log.i("MainActivity", "onPermissionsDenied");
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            new SettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onPermissionsGranted(int i, @NonNull List<String> list) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("MainActivity", "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @AfterPermissionGranted(
            REQUEST_CODE_PERMISSION
    )
    private void startProcess() {
        Log.i("MainActivity: ", "Permission Start");



        /*
         Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,

        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
        * */

        if (EasyPermissions.hasPermissions(this,  Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,

                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Already have permission, do the thing
            // ...

            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();

        } else {
            Log.i("MainActivity: ", "Permission request");

            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                     this,
                     "Need Permission",
                     REQUEST_CODE_PERMISSION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        }
    }


    @AfterPermissionGranted(
            REQUEST_CODE_PERMISSION
    )
    private void startProcessBelowS() {
        Log.i("MainActivity: ", "Permission Start");

        /*
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
         */

        if (EasyPermissions.hasPermissions(this,  Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Already have permission, do the thing
            // ...

            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();

        } else {
            Log.i("MainActivity: ", "Permission request");
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                    this,
                     "Need Permission",
                     REQUEST_CODE_PERMISSION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        }
    }

    private void er(){
//        DataInputStream ds;

    }





}