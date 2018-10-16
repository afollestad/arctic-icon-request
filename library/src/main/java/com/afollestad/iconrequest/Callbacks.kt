/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest

import android.net.Uri

typealias UriTransformer = ((Uri) -> (Uri))?

typealias ErrorCallback = ((Throwable) -> (Unit))?

typealias VoidCallback = (() -> (Unit))?

typealias LoadedCallback = ((List<AppModel>) -> (Unit))?

typealias SentCallback = ((Int) -> (Unit))?

typealias AppCallback = ((AppModel) -> (Unit))?

typealias LoadedAndErrorCallback = ((List<AppModel>, Throwable?) -> (Unit))?

typealias SentAndErrorCallback = ((Int, Throwable?) -> (Unit))?
