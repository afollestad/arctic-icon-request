/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest

import android.content.Context
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream

/** @author Aidan Follestad (afollestad) */
class AppIconLoaderFactory(private var context: Context) : ModelLoaderFactory<AppModel, InputStream> {

  override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AppModel, InputStream> {
    return AppIconLoader(context)
  }

  override fun teardown() {
  }
}
