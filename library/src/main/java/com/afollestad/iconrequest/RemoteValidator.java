package com.afollestad.iconrequest;

import android.support.annotation.NonNull;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;
import org.json.JSONObject;

/** @author Aidan Follestad (afollestad) */
class RemoteValidator extends ResponseValidator {

  @SuppressWarnings("ConstantConditions")
  @Override
  public boolean validate(@NonNull Response response) throws Exception {
    String body = response.asString();
    if (body != null && body.startsWith("{")) {
      JSONObject json = response.asJsonObject();
      if (!json.getString("status").equals("success")) throw new Exception(json.getString("error"));
    }
    return true;
  }

  @NonNull
  @Override
  public String id() {
    return "backend-validator";
  }
}
