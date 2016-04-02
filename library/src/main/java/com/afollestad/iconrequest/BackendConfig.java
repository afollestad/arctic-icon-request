package com.afollestad.iconrequest;

import java.io.Serializable;
import java.util.Locale;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class BackendConfig implements Serializable {

    public final String url;
    public final String apiKey;
    public boolean fallbackToEmail = false;

    public BackendConfig(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }

    public BackendConfig fallbackToEmail(boolean fallback) {
        fallbackToEmail = fallback;
        return this;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s - %s", url, apiKey);
    }
}