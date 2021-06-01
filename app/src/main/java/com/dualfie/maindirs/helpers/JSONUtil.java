package com.dualfie.maindirs.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.dualfie.maindirs.model.MessageFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class JSONUtil {

    public static String makeJSON(String bname, String message, String image) {
        JSONObject json = new JSONObject();

        try {
            json.put("message", message);
            json.put("from", bname );
            json.put("image", image );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    public static MessageFormat jsonMessage(String jsonData, boolean write) {
        try {

            JSONObject messageJSON = new JSONObject(jsonData);
            MessageFormat message = new MessageFormat(messageJSON.get("message").toString(),
                                         messageJSON.get("from").toString(),
                                         messageJSON.get("image").toString());
            return message;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
