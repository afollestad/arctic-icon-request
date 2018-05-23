package com.afollestad.iconrequest

/** @author Aidan Follestad (afollestad) */
internal interface SendInteractor {
  @Throws(Exception::class)
  fun send(
    selectedApps: List<AppModel>,
    request: ArcticRequest
  ): Boolean
}
