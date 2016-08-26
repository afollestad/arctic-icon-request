package com.afollestad.iconrequest;

import java.io.Serializable;
import java.util.Locale;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class BackendConfig implements Serializable {

    public final String url;
    public final String apiKey;
    public String sender;
    public boolean fallbackToEmail = false;

    public BackendConfig(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
        this.sender = "Anonymous";
    }

    public BackendConfig fallbackToEmail(boolean fallback) {
        fallbackToEmail = fallback;
        return this;
    }

    public BackendConfig sender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getSender() {
        if (sender == null || sender.trim().isEmpty())
            sender = "Anonymous";
        return sender;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s - %s", url, apiKey);
    }
}