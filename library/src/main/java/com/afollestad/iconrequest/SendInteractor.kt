/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest

import io.reactivex.Observable

/** @author Aidan Follestad (afollestad) */
internal interface SendInteractor {
  fun send(
    selectedApps: List<AppModel>,
    request: ArcticRequest
  ): Observable<Boolean>
}
