/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest.extensions

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

internal fun <T> Observable<T>.observeToMainThread() =
  observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.computation())

internal operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
  add(disposable)
}
