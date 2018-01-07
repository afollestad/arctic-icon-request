package com.afollestad.iconrequest;

import android.graphics.Bitmap;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** @author Aidan Follestad (afollestad) */
class FileUtil {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  static int wipe(File dir) {
    if (!dir.exists()) return 0;
    int count = 1;
    if (dir.isDirectory()) {
      File[] contents = dir.listFiles();
      if (contents != null && contents.length > 0) {
        for (File fi : contents) count += wipe(fi);
      }
    }
    dir.delete();
    return count;
  }

  static void writeIcon(File file, Bitmap icon) throws Exception {
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(file);
      icon.compress(Bitmap.CompressFormat.PNG, 100, os);
    } finally {
      FileUtil.closeQuietly(os);
    }
  }

  static void writeAll(File file, String content) throws Exception {
    writeAll(file, content.getBytes("UTF-8"));
  }

  static void writeAll(File file, byte[] content) throws Exception {
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
      os.write(content);
      os.flush();
    } finally {
      closeQuietly(os);
    }
  }

  static void closeQuietly(Closeable c) {
    try {
      c.close();
    } catch (Throwable ignored) {
    }
  }
}
