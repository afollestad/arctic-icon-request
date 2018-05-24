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

/** @author Aidan Follestad (afollestad) */
class ArcticRequest private constructor(context: Context) {

  private val loadingSubject: PublishSubject<Boolean> = PublishSubject.create()
  private val loadedSubject: PublishSubject<LoadResult> = PublishSubject.create()
  private val sendingSubject: PublishSubject<Boolean> = PublishSubject.create()
  private val sentSubject: PublishSubject<SendResult> = PublishSubject.create()
  private val selectionChangeSubject: PublishSubject<AppModel> = PublishSubject.create()

  private val appFilterSource: AppFilterSource
  private val componentInfoSource: ComponentInfoSource
  private val sendInteractor: SendInteractor
  internal var config: ArcticConfig = ArcticConfig()
  internal var uriTransformer: UriTransformer = { uri -> uri }

  private var loadedFilter: HashSet<String> = hashSetOf()
  private var storedLoadedApps: MutableList<AppModel> = mutableListOf()

  val selectedApps: Single<List<AppModel>>
    get() = Observable.just(storedLoadedApps)
        .flatMapIterable { it }
        .filter { it.selected }
        .toList()

  val requestedApps: Single<List<AppModel>>
    get() = Observable.just(storedLoadedApps)
        .flatMapIterable { it }
        .filter { it.requested }
        .toList()

  init {
    this.appFilterSource = AppFilterAssets(context)
    this.componentInfoSource = ComponentInfoPm(context)
    this.sendInteractor = RealSendInteractor(context)
  }

  fun setConfig(config: ArcticConfig): ArcticRequest {
    this.config = config
    return this
  }

  private fun restoreInstance(
    savedInstanceState: Bundle?
  ): ArcticRequest {
    if (savedInstanceState == null) {
      return this
    }

    config = savedInstanceState.getSerializable(KEY_CONFIG) as? ArcticConfig ?: ArcticConfig()
    loadedFilter = savedInstanceState.getSerializable(KEY_FILTER) as? HashSet<String> ?: HashSet(0)

    val loadedAppsArray = savedInstanceState.getSerializable(KEY_APPS) as? Array<AppModel>
    storedLoadedApps = loadedAppsArray?.toMutableList() ?: mutableListOf()
    if (storedLoadedApps.isNotEmpty()) {
      "Got ${storedLoadedApps.size} apps from restored instance state.".log(TAG)
      loadedSubject.onNext(LoadResult(storedLoadedApps))
      loadingSubject.onNext(false)
    }

    return this
  }

  fun saveInstance(out: Bundle?) {
    if (out == null) {
      return
    }
    out.putSerializable(KEY_CONFIG, config)
    out.putSerializable(KEY_FILTER, loadedFilter)
    out.putSerializable(KEY_APPS, storedLoadedApps.toTypedArray())
  }

  fun setUriTransformer(transformer: UriTransformer): ArcticRequest {
    this.uriTransformer = transformer
    return this
  }

  fun load(): Observable<LoadResult> {
    return Observable.fromCallable {
      loadingSubject.onNext(true)
      try {
        loadedFilter =
            appFilterSource.load(config!!.appFilterName, config!!.errorOnInvalidDrawables)!!
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
          loadingSubject.onNext(false)
          loadedSubject.onNext(it)
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
    selectionChangeSubject.onNext(app)
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
    selectionChangeSubject.onNext(app)
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
    selectionChangeSubject.onNext(app)
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
    loadedSubject.onNext(LoadResult(storedLoadedApps))
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
    loadedSubject.onNext(LoadResult(storedLoadedApps))
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
    loadedSubject.onNext(LoadResult(storedLoadedApps))
  }

  fun send(): Observable<SendResult> {
    return Observable.fromCallable {
      sendingSubject.onNext(true)
      selectedApps.blockingGet()
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
          sendingSubject.onNext(false)
          sentSubject.onNext(sendResult)
        }
  }

  fun loading(): Observable<Boolean> {
    return loadingSubject.observeToMainThread()
  }

  fun loaded(): Observable<LoadResult> {
    return loadedSubject.observeToMainThread()
  }

  fun sending(): Observable<Boolean> {
    return sendingSubject.observeToMainThread()
  }

  fun sent(): Observable<SendResult> {
    return sentSubject.observeToMainThread()
  }

  fun selectionChange(): Observable<AppModel> {
    return selectionChangeSubject.observeToMainThread()
  }

  companion object {

    private const val TAG = "ArcticRequest"
    private const val KEY_CONFIG = "ir.config"
    private const val KEY_FILTER = "ir.loadedFilter"
    private const val KEY_APPS = "ir.storedLoadedApps"

    @JvmStatic
    fun make(
      context: Context,
      savedInstanceState: Bundle?
    ): ArcticRequest {
      return ArcticRequest(context).restoreInstance(savedInstanceState)
    }
  }
}
