/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun Date.dateFormat() = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(this)
