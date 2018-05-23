package com.afollestad.iconrequest;

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.afollestad.iconrequest.extensions.closeQuietly
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** @author Aidan Follestad (afollestad) */
data class AppModel(
  val name: String,
  val code: String,
  val pkg: String,
  val requested: Boolean = false,
  val selected: Boolean = false
) {
  private fun getAppInfo(context: Context): ApplicationInfo? {
    return try {
      context.packageManager.getApplicationInfo(pkg, 0);
    } catch (e: PackageManager.NameNotFoundException) {
      null
    }
  }

  fun getIcon(context: Context): Drawable? {
    val appInfo = getAppInfo(context)
    return appInfo?.loadIcon(context.packageManager)
  }

  fun getIconStream(context: Context): InputStream? {
    val drawable = getIcon(context) ?: return null
    var os: ByteArrayOutputStream? = null
    try {
      val bmp =
        Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
      val canvas = Canvas(bmp)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas);
      os = ByteArrayOutputStream(bmp.byteCount)
      bmp.compress(Bitmap.CompressFormat.PNG, 100, os)
      return ByteArrayInputStream(os.toByteArray())
    } finally {
      os?.closeQuietly()
    }
  }
}