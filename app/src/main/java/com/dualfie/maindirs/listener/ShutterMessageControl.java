package com.dualfie.maindirs.listener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.dualfie.maindirs.dconstants.Constants;
import com.dualfie.maindirs.helpers.JSONUtil;
import com.dualfie.maindirs.model.MessageFormat;
import com.dualfie.maindirs.network.BluetoothComm;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ShutterMessageControl {
    private static final String TAG = "ShutterMessageControl";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private Handler mHandler;
    private BluetoothComm bluetoothMessageController;
    private ArrayList<MessageFormat> mChatMessages;
    private String mUser;
    private Bitmap mImage;
    private BluetoothSocket mSocket = null;


    protected void create(Context context) {
        mChatMessages = new ArrayList<MessageFormat>();
        mHandler = new Handler(new Handler.Callback() {
            JSONObject messageJSON = null;
            MessageFormat message2 = null;
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        Log.d(TAG, "State Change " + msg.arg1 );
                        switch (msg.arg1) {
                            case Constants.STATE_CONNECTED:
                                Toast.makeText(context, "Connected to " + mDevice.getName(),
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case Constants.STATE_CONNECTING:
                                Toast.makeText(context, "Connecting ",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case Constants.STATE_LISTEN:
                            case Constants.STATE_NONE:
                                Toast.makeText(context, "Connection Lost ",
                                        Toast.LENGTH_SHORT).show();
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        Log.d(TAG, "Message Write" );
                        byte[] writeBuf = (byte[]) msg.obj;
                        String writeMessage = new String(writeBuf);
                        JSONUtil.jsonMessage(writeMessage, true);
                        try {
                            messageJSON = new JSONObject(writeMessage);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            message2 = new MessageFormat(messageJSON.get("message").toString(),
                                    messageJSON.get("from").toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        break;
                    case Constants.MESSAGE_READ:
                        Log.d(TAG, "Message Read" );
                        byte[] readBuf = (byte[]) msg.obj;
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        JSONUtil.jsonMessage(readMessage, false);
                        try {
                            messageJSON = new JSONObject(readMessage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            message2 = new MessageFormat(messageJSON.get("message").toString(),
                                                             messageJSON.get("from").toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d( TAG, "Adding to the listview");
                        mChatMessages.add(message2);

                        break;
                    case Constants.MESSAGE_DEVICE_OBJECT:
                        Log.d(TAG, "Message Device Object" );
                        mDevice = msg.getData().getParcelable(Constants.DEVICE_OBJECT);
                        Toast.makeText(context, "Connected to " + mDevice.getName(),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_TOAST:
                        Log.d(TAG, "Message Toast" );
                        Toast.makeText(context, msg.getData().getString("toast"),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_LOST:
                        Log.d(TAG, "Message Lost" );
                        BluetoothComm.sleep(500);
                        Toast.makeText(context, "Reconnected", Toast.LENGTH_SHORT).show();
                        bluetoothMessageController.connect(mDevice);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + msg.what);
                }
                return false;
            }
        });

    }

   public ShutterMessageControl(Handler handler)
   {
       this.mHandler = handler;
   }

    public ShutterMessageControl()
    {



    }


    public void start(Context context) {
        create(context);
        bluetoothMessageController = new BluetoothComm(context,  mHandler);
        bluetoothMessageController.start();
    }

    public void Resume() {
        if (bluetoothMessageController != null) {
            if (bluetoothMessageController.getState() == Constants.STATE_NONE) bluetoothMessageController.start();
        }
    }
    public void Destroy() {
        if (bluetoothMessageController != null)
            bluetoothMessageController.stop();
    }

    public static final int SEND_IMAGE = 1;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendMessage(int img, String image, Context context) throws IOException {
        if (bluetoothMessageController.getState() != Constants.STATE_CONNECTED) {
            Toast.makeText( context, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }

       if (image.length() > 0) {
            if ( img == SEND_IMAGE) {
                new FiletoString() {
                    @Override
                    protected void onPostExecute(String buf) {
                        String send = JSONUtil.makeJSON("Image", "Image", buf);
                        bluetoothMessageController.write(send.getBytes());
                    }
                }.execute(image);
            }
            else {
                byte[] send = JSONUtil.makeJSON("message", "", image).getBytes();
                bluetoothMessageController.write(send);
            }
        }
    }

        private static class FiletoString extends AsyncTask<String, Integer, String> {
            @Override
            protected String doInBackground(String... params) {
                try {
                    return readFile(params[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                    cancel(true);
                }
                return null;
            }


            private static String readFile(String filename) throws IOException {
                File f = new File(filename);


                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                byte[] img = bytes.toByteArray();
                String encodedImage = Base64.encodeToString(img, Base64.DEFAULT);
                return encodedImage;
            }


    }



}
