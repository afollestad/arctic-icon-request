package com.afollestad.iconrequest;

import android.support.annotation.NonNull;

import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;

import org.json.JSONObject;

/**
 * @author Aidan Follestad (afollestad)
 */
class BackendValidator extends ResponseValidator {

    @Override
    public boolean validate(@NonNull Response response) throws Exception {
        if (response.headerEquals("Content-Type", "application/json")) {
            JSONObject json = response.asJsonObject();
            if (json == null)
                return false;
            else if (!json.getString("status").equals("success"))
                throw new Exception(json.getString("error"));
        }
        return true;
    }

    @NonNull
    @Override
    public String id() {
        return "backend-validator";
    }
}
