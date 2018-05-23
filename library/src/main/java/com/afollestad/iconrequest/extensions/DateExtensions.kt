package com.afollestad.iconrequest.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun Date.dateFormat(): String =
  SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(this)