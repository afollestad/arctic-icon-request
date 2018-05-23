package com.afollestad.iconrequest.extensions

import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.text.Spanned
import com.afollestad.iconrequest.AppModel
import io.reactivex.Observable
import java.util.Locale

internal val osVersionName: String
  get() {
    when (Build.VERSION.SDK_INT) {
      Build.VERSION_CODES.CUPCAKE -> return "Cupcake"
      Build.VERSION_CODES.DONUT -> return "Donut"
      Build.VERSION_CODES.ECLAIR, Build.VERSION_CODES.ECLAIR_0_1, Build.VERSION_CODES.ECLAIR_MR1 -> return "Eclair"
      Build.VERSION_CODES.FROYO -> return "Froyo"
      Build.VERSION_CODES.GINGERBREAD, Build.VERSION_CODES.GINGERBREAD_MR1 -> return "Gingerbread"
      Build.VERSION_CODES.HONEYCOMB, Build.VERSION_CODES.HONEYCOMB_MR1, Build.VERSION_CODES.HONEYCOMB_MR2 -> return "Honeycomb"
      Build.VERSION_CODES.ICE_CREAM_SANDWICH, Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 -> return "Ice Cream Sandwich"
      Build.VERSION_CODES.JELLY_BEAN, Build.VERSION_CODES.JELLY_BEAN_MR1, Build.VERSION_CODES.JELLY_BEAN_MR2 -> return "Jelly Bean"
      Build.VERSION_CODES.KITKAT -> return "KitKat"
      Build.VERSION_CODES.KITKAT_WATCH -> return "KitKat Watch"
      Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.LOLLIPOP_MR1 -> return "Lollipop"
      Build.VERSION_CODES.M -> return "Marshmallow"
      Build.VERSION_CODES.N, Build.VERSION_CODES.N_MR1 -> return "Nougat"
      Build.VERSION_CODES.O, Build.VERSION_CODES.O_MR1 -> return "Oreo"
      28 -> return "P"
      29 -> return "Q"
      30 -> return "R"
      31 -> return "S"
      32 -> return "T"
      33 -> return "U"
      34 -> return "V"
      35 -> return "W"
      36 -> return "X"
      37 -> return "Y"
      38 -> return "Z"
      else -> return ""
    }
  }

internal fun String.drawableName(): String {
  return toLowerCase(Locale.getDefault()).replace(" ", "_")
}

internal fun List<AppModel>.transferStates(
  to: MutableList<AppModel>
) {
  Observable.just(this)
      .flatMapIterable { it }
      .filter { it.selected }
      .forEach {
        for (i in to.indices) {
          val current = to[i]
          if (it.code == current.code) {
            to[i] = current.copy(selected = it.selected, requested = it.requested)
            break
          }
        }
      }
}

internal fun String.toHtml(): Spanned {
  return if (VERSION.SDK_INT >= VERSION_CODES.N) {
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY, null, null)
  } else {
    @Suppress("DEPRECATION")
    Html.fromHtml(this)
  }
}