package com.afollestad.iconrequest.extensions

import android.util.Log

fun String.log(
  tag: String? = null
) {
  Log.d(tag ?: "IconRequest", this)
}
