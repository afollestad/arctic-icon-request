package com.afollestad.iconrequest

import android.content.Context
import com.afollestad.iconrequest.extensions.wipe
import java.io.File
import java.io.Serializable

/** @author Aidan Follestad (afollestad) */
data class ArcticConfig(
  private val cacheFolder: String? = null,
  val appFilterName: String = "appfilter.xml",
  val emailRecipient: String? = null,
  val emailSubject: String? = "Icon Request",
  val emailHeader: String? = "These apps aren't themed on my device!",
  val emailFooter: String? = null,
  val includeDeviceInfo: Boolean = true,
  val errorOnInvalidDrawables: Boolean = true,
  val apiHost: String? = null,
  val apiKey: String? = null
): Serializable {
  fun actualCacheFolder(context: Context): File {
    return if (cacheFolder == null) {
      File(context.externalCacheDir, "com.afollestad.arctic").wipe()
    } else {
      File(cacheFolder).wipe()
    }
  }
}