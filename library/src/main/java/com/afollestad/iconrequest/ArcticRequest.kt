@file:Suppress("unused")

package com.afollestad.iconrequest

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.afollestad.iconrequest.extensions.log
import com.afollestad.iconrequest.extensions.observeToMainThread
import com.afollestad.iconrequest.extensions.plusAssign
import com.afollestad.iconrequest.extensions.transferStates
import com.afollestad.iconrequest.loaders.RealAppFilterSource
import com.afollestad.iconrequest.loaders.RealComponentInfoSource
import com.afollestad.iconrequest.remote.RealSendInteractor
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.HashSet

typealias UriTransformer = ((Uri) -> (Uri))?
typealias ErrorCallback = ((Throwable) -> (Unit))?
typealias VoidCallback = (() -> (Unit))?
typealias LoadedCallback = ((List<AppModel>) -> (Unit))?
typealias SentCallback = ((Int) -> (Unit))?
typealias AppCallback = ((AppModel) -> (Unit))?
typealias LoadedAndErrorCallback = ((List<AppModel>, Throwable?) -> (Unit))?
typealias SentAndErrorCallback = ((Int, Throwable?) -> (Unit))?

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

    this.loadedFilter = savedInstanceState?.getSerializable(KEY_FILTER) as? HashSet<String> ?:
        HashSet(0)
    val loadedAppsArray = savedInstanceState?.getSerializable(KEY_APPS) as? Array<AppModel>
    storedLoadedApps = loadedAppsArray?.toMutableList() ?: mutableListOf()
    if (storedLoadedApps.isNotEmpty()) {
      "Got ${storedLoadedApps.size} apps from restored instance state.".log(TAG)
      onLoaded?.invoke(storedLoadedApps)
    }
  }

  fun saveInstance(out: Bundle?) {
    if (out == null) return
    out.putSerializable(KEY_FILTER, loadedFilter)
    out.putSerializable(KEY_APPS, storedLoadedApps.toTypedArray())
  }

  fun performLoad(callback: LoadedAndErrorCallback = null) {
    onLoading?.invoke()
    disposables += Observable.just(true)
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
    if (index == -1) {
      throw IllegalArgumentException(
          "Unable to find app ${app.pkg} in this list of loaded apps!"
      )
    }
    return storedLoadedApps[index].selected
  }

  fun select(appToSelect: AppModel): ArcticRequest {
    var app = appToSelect
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
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
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
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
    val index = storedLoadedApps.indexOfFirst { it.code == app.code }
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
    onLoaded?.invoke(storedLoadedApps)
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
    onLoaded?.invoke(storedLoadedApps)
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
    onLoaded?.invoke(storedLoadedApps)
  }

  fun performSend(callback: SentAndErrorCallback = null) {
    onSending?.invoke()
    disposables += Observable.just(true)
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
            {
              "Failed to send the request! ${it.message}".log(TAG)
              if (onSendError != null || callback != null) {
                onSendError?.invoke(it)
                callback?.invoke(0, it)
              } else throw RuntimeException(it)
            }
        )
  }

  fun dispose() {
    disposables.dispose()
  }

  companion object {
    private const val TAG = "ArcticRequest"
    private const val KEY_FILTER = "ir.loadedFilter"
    private const val KEY_APPS = "ir.storedLoadedApps"
  }
}
