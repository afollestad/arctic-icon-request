package com.afollestad.iconrequest

import io.reactivex.Observable

/** @author Aidan Follestad (afollestad) */
internal interface SendInteractor {
  @Throws(Exception::class)
  fun send(
    selectedApps: List<AppModel>,
    request: ArcticRequest
  ): Observable<Boolean>
}
