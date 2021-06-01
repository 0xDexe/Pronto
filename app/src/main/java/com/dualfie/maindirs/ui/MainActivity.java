package com.dualfie.maindirs.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dualfie.maindirs.R;

public class MainActivity extends AppCompatActivity {

    String[] perm = new String[]{
            Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.VIBRATE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_master = findViewById(R.id.btn_master);
        btn_master.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent controlIntent = new Intent(getApplicationContext(), ControlActivity.class);
                finish();
                startActivity(controlIntent);
            }
        });
        Button btn_master_chat = findViewById(R.id.btn_master_chat);
        btn_master_chat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent controlIntent = new Intent(getApplicationContext(), ChatActivity.class);
                finish();
                startActivity(controlIntent);
            }
        });
        Button btn_shutter = findViewById(R.id.btn_localcamera);
        btn_shutter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent cameraIntent = new Intent(getApplicationContext(), Camera2BasicFragment.class);
                finish();
                startActivity(cameraIntent);
            }
        });

        if (!hasPermissions(this, perm)) {
            ActivityCompat.requestPermissions(this, perm, 1);
        }

    }

};






