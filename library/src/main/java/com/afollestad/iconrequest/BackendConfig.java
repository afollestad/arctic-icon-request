package com.afollestad.iconrequest;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class BackendConfig implements Serializable {

    public final String url;
    public final String apiKey;

    public BackendConfig(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }
}