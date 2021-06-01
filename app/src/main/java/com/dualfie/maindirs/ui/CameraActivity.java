package com.dualfie.maindirs.ui;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.dualfie.maindirs.R;
import com.dualfie.maindirs.model.MessageFormat;
import com.dualfie.maindirs.network.BluetoothComm;
import com.dualfie.maindirs.ui.view.MessageRecyclerView;

import java.util.ArrayList;

public class CameraActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.localcamera_main);
    }


}
