/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest.loaders

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.afollestad.iconrequest.AppModel
import com.afollestad.iconrequest.ComponentInfoSource
import com.afollestad.iconrequest.extensions.log
import java.util.Collections
import java.util.Comparator
import java.util.HashSet

/** @author Aidan Follestad (afollestad) */
internal class RealComponentInfoSource(private val context: Context) : ComponentInfoSource {

  override fun getInstalledApps(filter: HashSet<String>): MutableList<AppModel> {
    val pm = context.packageManager
    val appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    try {
      Collections.sort(
          appInfos,
          NameComparator(pm)
      )
    } catch (t: Throwable) {
      t.printStackTrace()
    }

    val apps = mutableListOf<AppModel>()
    var filtered = 0

    for (ai in appInfos) {
      val launchIntent = pm.getLaunchIntentForPackage(ai.packageName) ?: continue

      var launchStr = launchIntent.toString()
      launchStr = launchStr.substring(launchStr.indexOf("cmp=") + "cmp=".length)
      launchStr = launchStr.substring(0, launchStr.length - 2)

      val splitCode = launchStr.split("/".toRegex())
          .dropLastWhile { it.isEmpty() }
          .toTypedArray()
      if (splitCode[1].startsWith(".")) {
        launchStr = "${splitCode[0]}/${splitCode[0]}${splitCode[1]}"
      }

      if (filter.contains(launchStr)) {
        filtered++
        "Filtered $launchStr".log(TAG)
        continue
      }

      val name = ai.loadLabel(pm)
          .toString()
      apps.add(AppModel(name, launchStr, ai.packageName))
    }

    "Loaded ${apps.size} total app(s), filtered out $filtered app(s).".log(
        TAG
    )
    return apps
  }

  private class NameComparator internal constructor(private val packageManager: PackageManager) :
      Comparator<ApplicationInfo> {

    override fun compare(
      firstInfo: ApplicationInfo,
      secondInfo: ApplicationInfo
    ): Int {
      val sa: CharSequence = packageManager.getApplicationLabel(firstInfo) ?: firstInfo.packageName
      val sb: CharSequence =
        packageManager.getApplicationLabel(secondInfo) ?: secondInfo.packageName
      return sa.toString()
          .compareTo(sb.toString())
    }
  }

  companion object {
    private const val TAG = "RealComponentInfoSource"
  }
}
