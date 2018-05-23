package com.afollestad.iconrequest

import com.afollestad.bridge.Response
import com.afollestad.bridge.ResponseValidator
import org.json.JSONObject

/** @author Aidan Follestad (afollestad) */
internal class RemoteValidator : ResponseValidator() {
  @Throws(Exception::class)
  override fun validate(response: Response): Boolean {
    val body = response.asString()
    if (body != null && body.startsWith("{")) {
      val json = response.asJsonObject()
      if (json!!.getString("status") != "success") throw Exception(json.getString("error"))
    }
    return true
  }

  override fun id(): String {
    return "backend-validator"
  }
}
