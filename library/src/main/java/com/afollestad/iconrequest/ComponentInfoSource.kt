package com.afollestad.iconrequest

import java.util.HashSet

/** @author Aidan Follestad (afollestad) */
internal interface ComponentInfoSource {
  fun getInstalledApps(filter: HashSet<String>): MutableList<AppModel>
}
