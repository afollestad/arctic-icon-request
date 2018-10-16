/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequestsample

import android.content.Context
import com.afollestad.iconrequest.AppIconLoaderFactory
import com.afollestad.iconrequest.AppModel
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import java.io.InputStream

@GlideModule
class AppIconGlideModule : AppGlideModule() {
  override fun registerComponents(
    context: Context,
    glide: Glide,
    registry: Registry
  ) {
    super.registerComponents(context, glide, registry)
    registry.replace(AppModel::class.java, InputStream::class.java, AppIconLoaderFactory(context))
  }
}
