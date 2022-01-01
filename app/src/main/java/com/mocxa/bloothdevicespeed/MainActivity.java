package com.mocxa.bloothdevicespeed;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.mocxa.bloothdevicespeed.databinding.ActivityMainBinding;
import com.mocxa.bloothdevicespeed.device.DeviceActivity;
import com.mocxa.bloothdevicespeed.mobile.MobileActivity;

public class MainActivity extends AppCompatActivity {

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
//        setContentView(R.layout.activity_main);
    }

    private void startActivityMobile() {
        startActivity(new Intent(this, MobileActivity.class));

    }

    private void startActivityMobileDevice() {
        startActivity(new Intent(this, DeviceActivity.class));

    }

}