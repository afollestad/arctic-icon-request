package com.afollestad.iconrequest

/** @author Aidan Follestad (afollestad) */
data class SendResult(
  val sentCount: Int = 0,
  val usedArcticRm: Boolean = false,
  val error: Throwable? = null
) {
  val success: Boolean
    get() = error == null
}
