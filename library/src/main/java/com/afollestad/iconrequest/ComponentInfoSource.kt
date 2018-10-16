/*
 * Licensed under Apache-2.0
 *
 * Designed and developed by Aidan Follestad (@afollestad)
 */
package com.afollestad.iconrequest

import java.util.HashSet

/** @author Aidan Follestad (afollestad) */
internal interface ComponentInfoSource {
  fun getInstalledApps(filter: HashSet<String>): MutableList<AppModel>
}
