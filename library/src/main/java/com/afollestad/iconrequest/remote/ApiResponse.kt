/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest.remote

data class ApiResponse(
  var status: String = "success",
  var error: String? = null
)
