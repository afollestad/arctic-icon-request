package com.afollestad.iconrequest

/** @author Aidan Follestad (afollestad) */
data class LoadResult(
  val apps: List<AppModel> = listOf(),
  val error: Throwable? = null
) {
  val success: Boolean
    get() = error == null
}