package com.afollestad.iconrequest;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface RequestSendCallback {

    void onRequestPreparing();

    void onRequestError(Exception e);

    void onRequestSent();
}