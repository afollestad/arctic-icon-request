/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
@file:Suppress("unused")

package com.afollestad.iconrequest

import android.content.Context
import android.os.Bundle
import com.afollestad.iconrequest.extensions.log
import com.afollestad.iconrequest.extensions.observeToMainThread
import com.afollestad.iconrequest.extensions.plusAssign
import com.afollestad.iconrequest.extensions.toArrayList
import com.afollestad.iconrequest.extensions.transferStates
import com.afollestad.iconrequest.loaders.RealAppFilterSource
import com.afollestad.iconrequest.loaders.RealComponentInfoSource
import com.afollestad.iconrequest.remote.ApiResponse
import com.afollestad.iconrequest.remote.RealSendInteractor
import io.reactivex.Observable.just
import io.reactivex.disposables.CompositeDisposable
import retrofit2.HttpException
import java.util.HashSet

/** @author Aidan Follestad (afollestad) */
class ArcticRequest constructor(
  context: Context,
  savedInstanceState: Bundle? = null,
  internal val config: ArcticConfig = ArcticConfig(),
  internal val uriTransformer: UriTransformer = null,
  private val onSending: VoidCallback = null,
  private val onSendError: ErrorCallback = null,
  private val onSent: SentCallback = null,
  private val onLoading: VoidCallback = null,
  private val onLoadError: ErrorCallback = null,
  private val onLoaded: LoadedCallback = null,
  private val onSelectionChange: AppCallback = null
) {

  companion object {
    private const val TAG = "ArcticRequest"
    private const val KEY_FILTER = "ir.loadedFilter"
    private const val KEY_APPS = "ir.storedLoadedApps"
  }

  private val appFilterSource: AppFilterSource
  private val componentInfoSource: ComponentInfoSource
  private val sendInteractor: SendInteractor
  private val disposables = CompositeDisposable()

  private var loadedFilter: HashSet<String>
  private var storedLoadedApps: MutableList<AppModel>

  val selectedApps: List<AppModel>
    get() = loadedApps.filter { it.selected }

  val requestedApps: List<AppModel>
    get() = loadedApps.filter { it.requested }

  val loadedApps: List<AppModel>
    get() = storedLoadedApps.toList()

  init {
    this.appFilterSource = RealAppFilterSource(context)
    this.componentInfoSource = RealComponentInfoSource(context)
    this.sendInteractor = RealSendInteractor(context)

    @Suppress("UNCHECKED_CAST")
    this.loadedFilter = savedInstanceState?.getSerializable(KEY_FILTER) as? HashSet<String>
        ?: HashSet(0)

    val restoredApps = savedInstanceState?.getParcelableArrayList<AppModel>(KEY_APPS)
    storedLoadedApps = restoredApps?.toMutableList() ?: mutableListOf()
    if (storedLoadedApps.isNotEmpty()) {
      "Got ${storedLoadedApps.size} apps from restored instance state.".log(TAG)
      onLoaded?.invoke(storedLoadedApps)
    }
  }

  fun saveInstance(out: Bundle?) {
    if (out == null) return
    out.putSerializable(KEY_FILTER, loadedFilter)
    out.putParcelableArrayList(KEY_APPS, storedLoadedApps.toArrayList())
  }

  fun performLoad(callback: LoadedAndErrorCallback = null) {
    onLoading?.invoke()
    disposables += just(true)
        .flatMap {
          appFilterSource.load(config.appFilterName, config.errorOnInvalidDrawables)
        }
        .map {
          loadedFilter = it
          val newLoadedApps = componentInfoSource.getInstalledApps(loadedFilter)
          if (!storedLoadedApps.isEmpty()) {
            storedLoadedApps.transferStates(newLoadedApps)
          }
          storedLoadedApps = newLoadedApps
          storedLoadedApps
        }
        .observeToMainThread()
        .subscribe(
            {
              onLoaded?.invoke(it)
              callback?.invoke(it, null)
            },
            {
              if (onLoadError != null || callback != null) {
                onLoadError?.invoke(it)
                callback?.invoke(listOf(), it)
              } else throw RuntimeException(it)
            }
        )
  }

  fun isSelected(app: AppModel): Boolean {
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
    check(index != -1) {
      "Unable to find app ${app.pkg} in this list of loaded apps!"
    }
    return storedLoadedApps[index].selected
  }

  fun select(appToSelect: AppModel): ArcticRequest {
    var app = appToSelect
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
    check(index != -1) {
      "Unable to find app ${app.pkg} in this list of loaded apps!"
    }

    app = storedLoadedApps[index]
    if (app.selected) {
      return this
    }

    app = app.copy(selected = true)
    storedLoadedApps[index] = app
    onSelectionChange?.invoke(app)
    return this
  }

  fun deselect(appToDeselect: AppModel): ArcticRequest {
    var app = appToDeselect
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
    check(index != -1) {
      "Unable to find app ${app.pkg} in this list of loaded apps!"
    }

    app = storedLoadedApps[index]
    if (!app.selected) {
      return this
    }

    app = app.copy(selected = false)
    storedLoadedApps[index] = app
    onSelectionChange?.invoke(app)
    return this
  }

  fun toggleSelection(appToToggle: AppModel): ArcticRequest {
    var app = appToToggle
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
    check(index != -1) {
      "Unable to find app ${app.pkg} in this list of loaded apps!"
    }

    app = storedLoadedApps[index]
    // Toggle selection state
    app = app.copy(selected = !app.selected)
    storedLoadedApps[index] = app
    onSelectionChange?.invoke(app)
    return this
  }

  fun selectAll(): ArcticRequest {
    for ((i, app) in storedLoadedApps.withIndex()) {
      storedLoadedApps[i] = app.copy(selected = true)
    }
    onLoaded?.invoke(storedLoadedApps)
    return this
  }

  fun deselectAll(): ArcticRequest {
    for ((i, app) in storedLoadedApps.withIndex()) {
      storedLoadedApps[i] = app.copy(selected = false)
    }
    onLoaded?.invoke(storedLoadedApps)
    return this
  }

  fun performSend(callback: SentAndErrorCallback = null) {
    onSending?.invoke()
    disposables += just(true)
        .map { selectedApps }
        .flatMap { selectedApps ->
          sendInteractor.send(selectedApps, this@ArcticRequest)
              .map { selectedApps.size }
        }
        .observeToMainThread()
        .subscribe(
            {
              "Request sent successfully!".log(TAG)
              resetSelection()
              onSent?.invoke(it)
              callback?.invoke(it, null)
            },
            { processError(it, callback) }
        )
  }

  fun dispose() = disposables.dispose()

  private fun resetSelection() {
    val seq = storedLoadedApps.asSequence()
        .filter { it.selected }
    for ((i, app) in seq.withIndex().toList()) {
      storedLoadedApps[i] = app.copy(selected = false, requested = true)
    }
    onLoaded?.invoke(storedLoadedApps)
  }

  private fun processError(
    error: Throwable,
    callback: SentAndErrorCallback
  ) {
    if (error is HttpException) {
      val errorBody = error.response().errorBody()!!.string()
      if (errorBody.isEmpty()) {
        val errorEx = IllegalStateException("HTTP Status ${error.response().code()}")
        if (onSendError != null || callback != null) {
          onSendError?.invoke(errorEx)
          callback?.invoke(0, errorEx)
          return
        } else throw RuntimeException(errorEx)
      }

      val errorResponse = RealSendInteractor.getGson()
          .fromJson(errorBody, ApiResponse::class.java)
      "Got an error response from the API! ${errorResponse.error}".log(TAG)
      val errorEx = Exception(errorResponse.error)
      if (onSendError != null || callback != null) {
        onSendError?.invoke(errorEx)
        callback?.invoke(0, errorEx)
      } else throw RuntimeException(errorEx)

      return
    }

    "Failed to send the request! ${error.message}".log(TAG)
    if (onSendError != null || callback != null) {
      onSendError?.invoke(error)
      callback?.invoke(0, error)
    } else throw RuntimeException(error)
  }
}
