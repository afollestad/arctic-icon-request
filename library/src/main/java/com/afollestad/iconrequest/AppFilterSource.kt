package com.afollestad.iconrequest

import io.reactivex.Observable
import java.util.HashSet

/** @author Aidan Follestad (afollestad) */
internal interface AppFilterSource {
  fun load(
    filterName: String,
    errorOnInvalidDrawables: Boolean
  ): Observable<HashSet<String>>
}
