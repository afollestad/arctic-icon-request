package com.afollestad.iconrequest.remote

data class ApiResponse(
  var status: String = "success",
  var error: String? = null
)