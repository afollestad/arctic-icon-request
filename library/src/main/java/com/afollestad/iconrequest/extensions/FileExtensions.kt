package com.afollestad.iconrequest.extensions

import android.graphics.Bitmap
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

internal fun File.wipe(): File {
  if (!exists()) return this
  if (isDirectory) {
    val contents = listFiles()
    if (contents != null && contents.isNotEmpty()) {
      for (fi in contents) fi.wipe()
    }
  }
  delete()
  return this
}

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

@Throws(Exception::class)
internal fun String.writeAll(file: File) {
  toByteArray(charset("UTF-8")).writeAll(file)
}

@Throws(Exception::class)
internal fun ByteArray.writeAll(file: File) {
  var os: OutputStream? = null
  try {
    os = FileOutputStream(file)
    os.write(this)
    os.flush()
  } finally {
    os?.closeQuietly()
  }
}

internal fun Closeable.closeQuietly() {
  try {
    close()
  } catch (ignored: Throwable) {
  }
}