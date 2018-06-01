package com.afollestad.iconrequest.remote

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

class ApiTypeAdapter : TypeAdapter<ApiResponse>() {
  companion object {
    private const val KEY_STATUS = "status"
    private const val KEY_ERROR = "error"
  }

  @Throws(IOException::class)
  override fun write(
    out: JsonWriter,
    value: ApiResponse
  ) {
    out.beginObject()
    out.name(KEY_STATUS)
        .value(value.status)
    out.name(KEY_ERROR)
        .value(value.error)
    out.endObject()
  }

  @Throws(IOException::class)
  override fun read(input: JsonReader): ApiResponse? {
    input.beginObject()
    var status = "success"
    var error: String? = null
    while (input.hasNext()) {
      val next = input.nextString()
      when (next) {
        "status" -> status = next
        "error" -> error = next
      }
    }
    input.endObject()
    return ApiResponse(status = status, error = error)
  }
}
