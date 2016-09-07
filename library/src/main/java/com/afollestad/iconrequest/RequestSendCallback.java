package com.afollestad.iconrequest;

import android.net.Uri;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface RequestSendCallback {

    void onRequestPreparing();

    void onRequestError(Exception e);

    Uri onRequestProcessUri(Uri uri);

    void onRequestSent();
}