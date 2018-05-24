package com.afollestad.iconrequest.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream

@Throws(Exception::class)
internal fun Bitmap.writeIconTo(file: File) {
  var os: FileOutputStream? = null
  try {
    os = FileOutputStream(file)
    compress(Bitmap.CompressFormat.PNG, 100, os)
  } finally {
    os?.closeQuietly()
  }
}

internal fun Drawable.toBitmap(): Bitmap {
  return if (this is BitmapDrawable) {
    this.bitmap
  } else {
    val bmp = Bitmap.createBitmap(
        this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    bmp
  }
}