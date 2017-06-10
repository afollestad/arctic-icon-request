package com.afollestad.iconrequest;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Locale;

/** @author Aidan Follestad (afollestad) */
class IRUtils {

  static boolean isEmpty(@Nullable String str) {
    return str == null || str.trim().isEmpty();
  }

  static boolean inClassPath(@NonNull String clsName) {
    try {
      Class.forName(clsName);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  static String drawableName(String appName) {
    return appName.toLowerCase(Locale.getDefault()).replace(" ", "_");
  }

  static String getOSVersionName(int sdkInt) {
    switch (sdkInt) {
      default:
        return "";
      case Build.VERSION_CODES.CUPCAKE:
        return "Cupcake";
      case Build.VERSION_CODES.DONUT:
        return "Donut";
      case Build.VERSION_CODES.ECLAIR:
      case Build.VERSION_CODES.ECLAIR_0_1:
      case Build.VERSION_CODES.ECLAIR_MR1:
        return "Eclair";
      case Build.VERSION_CODES.FROYO:
        return "Froyo";
      case Build.VERSION_CODES.GINGERBREAD:
      case Build.VERSION_CODES.GINGERBREAD_MR1:
        return "Gingerbread";
      case Build.VERSION_CODES.HONEYCOMB:
      case Build.VERSION_CODES.HONEYCOMB_MR1:
      case Build.VERSION_CODES.HONEYCOMB_MR2:
        return "Honeycomb";
      case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
      case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
        return "Ice Cream Sandwich";
      case Build.VERSION_CODES.JELLY_BEAN:
      case Build.VERSION_CODES.JELLY_BEAN_MR1:
      case Build.VERSION_CODES.JELLY_BEAN_MR2:
        return "Jelly Bean";
      case Build.VERSION_CODES.KITKAT:
        return "KitKat";
      case Build.VERSION_CODES.KITKAT_WATCH:
        return "KitKat Watch";
      case Build.VERSION_CODES.LOLLIPOP:
      case Build.VERSION_CODES.LOLLIPOP_MR1:
        return "Lollipop";
      case Build.VERSION_CODES.M:
        return "Marshmallow";
      case Build.VERSION_CODES.N:
        return "Nougat";
      case 25:
        return "O";
      case 26:
        return "P";
      case 27:
        return "Q";
      case 28:
        return "R";
      case 29:
        return "S";
      case 30:
        return "T";
      case 31:
        return "U";
      case 32:
        return "V";
      case 33:
        return "W";
      case 34:
        return "X";
      case 35:
        return "Y";
      case 36:
        return "Z";
    }
  }
}
