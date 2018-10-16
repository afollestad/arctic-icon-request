/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

/** @author Aidan Follestad (afollestad) */
internal class AppIconLoader(private val context: Context) : ModelLoader<AppModel, InputStream> {

  override fun buildLoadData(
    appModel: AppModel,
    width: Int,
    height: Int,
    options: Options
  ): ModelLoader.LoadData<InputStream>? {
    return ModelLoader.LoadData(ObjectKey(appModel.pkg), AppIconDataFetcher(context, appModel))
  }

  override fun handles(appModel: AppModel): Boolean {
    return true
  }

  private inner class AppIconDataFetcher internal constructor(
    private var context: Context,
    private var model: AppModel?
  ) : DataFetcher<InputStream> {

    override fun loadData(
      priority: Priority,
      callback: DataFetcher.DataCallback<in InputStream>
    ) {
      try {
        callback.onDataReady(model!!.getIconStream(context))
      } catch (e: Exception) {
        callback.onLoadFailed(e)
      }
    }

    override fun cleanup() {
      this.model = null
    }

    override fun cancel() {}

    override fun getDataClass(): Class<InputStream> {
      return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
      return DataSource.REMOTE
    }
  }
}
