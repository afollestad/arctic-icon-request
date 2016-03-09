package com.afollestad.iconrequest;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class BackendConfig implements Serializable {

    public final String url;
    public final String appId;

    public BackendConfig(String url, String appId) {
        this.url = url;
        this.appId = appId;
    }
}