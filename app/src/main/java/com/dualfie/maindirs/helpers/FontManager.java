package com.dualfie.maindirs.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;

public class FontManager {
    public static final String ROOT = "fonts/";

    public static Typeface getTypeface(Context context, String font) {
        return Typeface.createFromAsset(context.getAssets(), ROOT + font);
    }

    public static String setIcon(Context context, int id) {
        String result = "";
        result = Html.fromHtml(context.getString(id)).toString();
        return result;
    }
}
