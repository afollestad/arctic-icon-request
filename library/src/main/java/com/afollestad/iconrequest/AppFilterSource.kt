package com.afollestad.iconrequest

import java.util.HashSet

/** @author Aidan Follestad (afollestad) */
internal interface AppFilterSource {
  @Throws(Exception::class)
  fun load(
    filterName: String,
    errorOnInvalidDrawables: Boolean
  ): HashSet<String>?
}
