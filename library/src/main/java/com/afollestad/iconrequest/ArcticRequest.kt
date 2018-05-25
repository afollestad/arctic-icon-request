@file:Suppress("unused")

package com.afollestad.iconrequest

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.afollestad.iconrequest.extensions.log
import com.afollestad.iconrequest.extensions.observeToMainThread
import com.afollestad.iconrequest.extensions.transferStates
import com.afollestad.iconrequest.remote.RealSendInteractor
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList
import java.util.HashSet

typealias UriTransformer = ((Uri) -> (Uri))?
typealias VoidCallback = (() -> (Unit))?
typealias LoadedCallback = ((LoadResult) -> (Unit))?
typealias SentCallback = ((SendResult) -> (Unit))?
typealias AppCallback = ((AppModel) -> (Unit))?

/** @author Aidan Follestad (afollestad) */
class ArcticRequest constructor(
  context: Context,
  savedInstanceState: Bundle? = null,
  internal val config: ArcticConfig = ArcticConfig(),
  internal val uriTransformer: UriTransformer = null,
  private val onSending: VoidCallback = null,
  private val onSent: SentCallback = null,
  private val onLoading: VoidCallback = null,
  private val onLoaded: LoadedCallback = null,
  private val onSelectionChange: AppCallback = null
) {

  private val appFilterSource: AppFilterSource
  private val componentInfoSource: ComponentInfoSource
  private val sendInteractor: SendInteractor

  private var loadedFilter: HashSet<String>
  private var storedLoadedApps: MutableList<AppModel>

  val selectedApps: List<AppModel>
    get() = storedLoadedApps.filter { it.selected }

  val requestedApps: List<AppModel>
    get() = storedLoadedApps.filter { it.requested }

  init {
    this.appFilterSource = AppFilterAssets(context)
    this.componentInfoSource = ComponentInfoPm(context)
    this.sendInteractor = RealSendInteractor(context)

    this.loadedFilter = savedInstanceState?.getSerializable(KEY_FILTER) as? HashSet<String> ?:
        HashSet(0)
    val loadedAppsArray = savedInstanceState?.getSerializable(KEY_APPS) as? Array<AppModel>
    storedLoadedApps = loadedAppsArray?.toMutableList() ?: mutableListOf()
    if (storedLoadedApps.isNotEmpty()) {
      "Got ${storedLoadedApps.size} apps from restored instance state.".log(TAG)
      onLoaded?.invoke(LoadResult(storedLoadedApps))
    }
  }

  fun saveInstance(out: Bundle?) {
    if (out == null) return
    out.putSerializable(KEY_FILTER, loadedFilter)
    out.putSerializable(KEY_APPS, storedLoadedApps.toTypedArray())
  }

  fun load(): Observable<LoadResult> {
    return Observable.fromCallable {
      onLoading?.invoke()
      try {
        loadedFilter =
            appFilterSource.load(config.appFilterName, config.errorOnInvalidDrawables)!!
      } catch (e: Exception) {
        return@fromCallable LoadResult(error = e)
      }

      val newLoadedApps = componentInfoSource.getInstalledApps(loadedFilter)
      if (!storedLoadedApps.isEmpty()) {
        storedLoadedApps.transferStates(newLoadedApps)
      }
      storedLoadedApps = newLoadedApps
      LoadResult(storedLoadedApps)
    }
        .observeToMainThread()
        .doOnNext {
          onLoaded?.invoke(it)
        }
  }

  fun getLoadedApps(): List<AppModel> {
    return storedLoadedApps.toList()
  }

  fun isSelected(app: AppModel): Boolean {
    val index = storedLoadedApps.indexOf(app)
    if (index == -1) {
      throw IllegalArgumentException(
          "Unable to find app ${app.pkg} in this list of loaded apps!"
      )
    }
    return storedLoadedApps[index].selected
  }

  fun select(appToSelect: AppModel): ArcticRequest {
    var app = appToSelect
    val index = storedLoadedApps.indexOf(app)
    if (index == -1) {
      throw IllegalArgumentException(
          "Unable to find app ${app.pkg} in this list of loaded apps!"
      )
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
    val index = storedLoadedApps.indexOf(app)
    if (index == -1) {
      throw IllegalArgumentException(
          "Unable to find app ${app.pkg} in this list of loaded apps!"
      )
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
    val index = storedLoadedApps.indexOf(app)
    if (index == -1) {
      throw IllegalArgumentException(
          "Unable to find app ${app.pkg} in this list of loaded apps!"
      )
    }
    app = storedLoadedApps[index]
    // Toggle selection state
    app = app.copy(selected = !app.selected)
    storedLoadedApps[index] = app
    onSelectionChange?.invoke(app)
    return this
  }

  fun selectAll(): ArcticRequest {
    for (i in storedLoadedApps.indices) {
      var app = storedLoadedApps[i]
      if (app.selected) {
        continue
      }
      app = app.copy(selected = true)
      storedLoadedApps[i] = app
    }
    onLoaded?.invoke(LoadResult(storedLoadedApps))
    return this
  }

  fun deselectAll(): ArcticRequest {
    for (i in storedLoadedApps.indices) {
      var app = storedLoadedApps[i]
      if (!app.selected) {
        continue
      }
      app = app.copy(selected = false)
      storedLoadedApps[i] = app
    }
    onLoaded?.invoke(LoadResult(storedLoadedApps))
    return this
  }

  private fun resetSelection() {
    for (i in storedLoadedApps.indices) {
      var app = storedLoadedApps[i]
      if (!app.selected) {
        continue
      }
      app = app.copy(selected = false, requested = true)
      storedLoadedApps[i] = app
    }
    onLoaded?.invoke(LoadResult(storedLoadedApps))
  }

  fun send(): Observable<SendResult> {
    return Observable.fromCallable {
      onSending?.invoke()
      selectedApps
    }
        .flatMap { selectedApps ->
          sendInteractor.send(selectedApps, this@ArcticRequest)
              .map { Pair(selectedApps.size, it) }
        }
        .map { SendResult(it.first, it.second) }
        .onErrorReturn { SendResult(0, false, it) }
        .observeToMainThread()
        .doOnNext { sendResult ->
          resetSelection()
          onSent?.invoke(sendResult)
        }
  }

  companion object {
    private const val TAG = "ArcticRequest"
    private const val KEY_FILTER = "ir.loadedFilter"
    private const val KEY_APPS = "ir.storedLoadedApps"
  }
}
