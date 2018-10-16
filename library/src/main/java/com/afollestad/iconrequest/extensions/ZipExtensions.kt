/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest.extensions

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Throws(Exception::class)
internal fun Collection<File>.zipInto(
  intoZipFile: File
) {
  var outputStream: ZipOutputStream? = null
  var inputStream: InputStream? = null
  try {
    outputStream = ZipOutputStream(FileOutputStream(intoZipFile))
    for (fi in this) {
      outputStream.putNextEntry(ZipEntry(fi.name))
      inputStream = FileInputStream(fi)
      inputStream.copyTo(outputStream, 2048)
      inputStream.closeQuietly()
      outputStream.closeEntry()
    }
  } finally {
    inputStream?.closeQuietly()
    outputStream?.closeQuietly()
  }
}
