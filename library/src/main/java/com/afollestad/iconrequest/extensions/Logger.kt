/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest.extensions

import android.util.Log

fun String.log(
  tag: String? = null
) = Log.d(tag ?: "IconRequest", this)
