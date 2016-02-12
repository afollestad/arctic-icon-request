package com.afollestad.iconrequest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * @author Aidan Follestad (afollestad)
 */
class IRLog {

    public static void log(@Nullable String tag, @NonNull String msg, @Nullable Object... args) {
        if(tag == null) tag = "IconRequest";
        if (args != null)
            msg = String.format(msg, args);
        Log.d(tag, msg);
    }

    private IRLog() {
    }
}
