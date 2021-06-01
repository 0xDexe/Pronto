package com.dualfie.maindirs.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dualfie.maindirs.R;

import com.dualfie.maindirs.helpers.JSONUtil;
import com.dualfie.maindirs.dconstants.Constants;
import com.dualfie.maindirs.model.MessageFormat;
import com.dualfie.maindirs.network.BluetoothComm;
import com.dualfie.maindirs.ui.view.BluetoothDevicesListView;
import com.dualfie.maindirs.ui.view.MessageRecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import butterknife.ButterKnife;

public class ControlActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ControlActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private Handler mHandler;
    private BluetoothComm bluetoothMessageController;
    private MessageRecyclerView mControlAdapter;
    private ArrayList<MessageFormat> mControlMessages;


    private LinearLayoutManager mLinearLayoutManager;
    private Bitmap mImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_main);

        Button mStart = findViewById(R.id.btn_start);
        Button mCapture = findViewById(R.id.btn_capture);
        Button mStop = findViewById(R.id.btn_stop);
        RecyclerView mListView= findViewById(R.id.list_of_messages);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }
        else if (!mBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BLUETOOTH);
        }

        mControlMessages = new ArrayList<MessageFormat>();
        mControlAdapter = new MessageRecyclerView(this, mControlMessages);
        mListView.setAdapter(mControlAdapter);

        mLinearLayoutManager = new LinearLayoutManager(this);
        mListView.setLayoutManager(mLinearLayoutManager);

        mHandler = new Handler(new Handler.Callback() {
            JSONObject messageJSON = null;
            MessageFormat message2 = null;
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case Constants.STATE_CONNECTED:
                                setTitle(mDevice.getName());
                                break;
                            case Constants.STATE_CONNECTING:
                                setTitle("Connecting...");
                                break;
                            case Constants.STATE_LISTEN:
                            case Constants.STATE_NONE:
                                setTitle("Not connected to the Shutter");
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        Log.d(TAG, "Message Write" );
                        byte[] writeBuf = (byte[]) msg.obj;
                        String writeMessage = new String(writeBuf);
                        try {
                            messageJSON = new JSONObject(writeMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            message2 = new MessageFormat(messageJSON.get("message").toString(),
                                    messageJSON.get("from").toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d( TAG, "Adding to the listview - Master");
                        mControlMessages.add(message2);
                        mControlAdapter.notifyDataSetChanged();

                        break;

                    // Read message from the peer and add to the list view - takes images also
                    case Constants.MESSAGE_READ:
                        Log.d(TAG, "Message Read" );

                        String readMessage = msg.obj.toString();
                        try {
                            messageJSON = new JSONObject(readMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            message2 = new MessageFormat(messageJSON.get("message").toString(),
                                    messageJSON.get("from").toString(), messageJSON.get("image").toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d( TAG, "Adding to the listview");
                          mControlMessages.add(message2);
                          mControlAdapter.notifyDataSetChanged();
                        break;
                    case Constants.MESSAGE_DEVICE_OBJECT:
                        Log.d(TAG, "Message Device Object" );
                        mDevice = msg.getData().getParcelable(Constants.DEVICE_OBJECT);
                        Toast.makeText(getApplicationContext(), "Connected to " + mDevice.getName(),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_TOAST:
                        Log.d(TAG, "Message Toast" );
                        Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_LOST:
                        Log.d(TAG, "Message Lost" );
                        BluetoothComm.sleep(500);
                        Toast.makeText(getApplicationContext(), "Reconnected", Toast.LENGTH_SHORT).show();
                        bluetoothMessageController.connect(mDevice);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + msg.what);
                }
              return false;
            }
        });

        mStart.setOnClickListener(this);
        mCapture.setOnClickListener(this);
        mStop.setOnClickListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bluetooth) {
            bluetoothSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void bluetoothSearch() {
        BluetoothDevicesListView display = new BluetoothDevicesListView(mBluetoothAdapter, bluetoothMessageController);
        display.show(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BLUETOOTH);
        } else {
            bluetoothMessageController = new BluetoothComm(this, mHandler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (bluetoothMessageController != null) {
            if (bluetoothMessageController.getState() == Constants.STATE_NONE)
                bluetoothMessageController.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothMessageController != null)
            bluetoothMessageController.stop();
    }


    private void sendMessage(String message) {
        if (bluetoothMessageController.getState() != Constants.STATE_CONNECTED) {
            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() > 0) {
            byte[] send = JSONUtil.makeJSON(mBluetoothAdapter.getName(), message, "").getBytes();
            bluetoothMessageController.write(send);

        }
    }

    @Override
    public void onClick(View v) {

            switch(v.getId())
            {
                case R.id.btn_start:
                    sendMessage("START");
                    break;
                case R.id.btn_capture:
                    sendMessage("CAPTURE");
                    break;
                case R.id.btn_stop:
                    sendMessage("STOP");
                    break;

            }


        }
    }

