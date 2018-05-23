package com.afollestad.iconrequest.extensions

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

internal fun <T> Observable<T>.observeToMainThread() =
  observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.computation())