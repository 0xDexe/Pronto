package com.dualfie.maindirs.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.dualfie.maindirs.dconstants.Constants;

import static com.dualfie.maindirs.dconstants.Constants.STATE_CONNECTED;


public class BluetoothComm {
    private static final String TAG = "BlueToothComm";
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private Accept mAcceptThread;
    private Connect mConnectThread;
    private ReadWrite mConnectedThread;
    private int mState;
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    //  private static final UUID MY_UUID =
    //         UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public BluetoothComm(Context context, Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = Constants.STATE_NONE;
        this.mHandler = handler;
    }

    // Set the current state
    private void setState(int state) {
        this.mState = state;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public int getState() {
        return mState;
    }

    public BluetoothAdapter getBluetoothAdapter( )
    {
        return mBluetoothAdapter;
    }

    public void start() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        setState(Constants.STATE_LISTEN);
        if (mAcceptThread == null) {
            mAcceptThread = new Accept();
            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        if (mState == Constants.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        setState(Constants.STATE_CONNECTING);
        mConnectThread = new Connect(device);
        mConnectThread.start();

    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connected(BluetoothSocket socket, BluetoothDevice device) {

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ReadWrite(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_OBJECT);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.DEVICE_OBJECT, device);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(Constants.STATE_CONNECTED);
    }


    public void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }


        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(Constants.STATE_NONE);
    }

    public void write(byte[] out) {
        ReadWrite r;
        synchronized (this) {
            if (mState != Constants.STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        r.write(out);
    }


    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        BluetoothComm.this.start();
    }

    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        BluetoothComm.this.start();
    }


    public class Accept extends Thread {
        private BluetoothServerSocket mServerSocket;

        public Accept() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("chat-chat", MY_UUID);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket;
            while (mState != STATE_CONNECTED) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothComm.this) {
                        switch (mState) {
                            case Constants.STATE_LISTEN:
                            case Constants.STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case Constants.STATE_NONE:
                            case Constants.STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public class Connect extends Thread {
        private BluetoothSocket mSocket;
        private BluetoothDevice mDevice;

        public Connect(BluetoothDevice device) {
            this.mDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothComm.this) {
                mConnectThread = null;
            }

            connected(mSocket, mDevice);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public class ReadWrite extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ReadWrite(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }


       /* public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            int x= 0;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    x += bytes;
                    Log.d( TAG, "Read Bytes " + x );
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    BluetoothComm.this.start();
                    break;
                }
            }
        }
        */
        public void run() {
            String inString = "";
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            int len = 0;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String chunk = new String(buffer, 0, bytes);
                    if (chunk.contains("}")) {
                        inString += chunk.substring(0, chunk.indexOf('}'));
                        inString += '}';
                        mHandler.obtainMessage(Constants.MESSAGE_READ, inString.length(), -1,
                                inString).sendToTarget();
                        inString = chunk.substring(chunk.indexOf('}'));
                    } else {
                        inString += chunk;
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }


        public void write(byte[] buffer) {
            Log.d(TAG, "Message Write" );
            try {
                outputStream.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE,buffer.length, -1,
                        buffer).sendToTarget();
                outputStream.flush();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
