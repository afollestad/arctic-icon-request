package com.afollestad.iconrequest;

import static com.afollestad.iconrequest.IRLog.log;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** @author Aidan Follestad (afollestad) */
class ComponentInfoPm implements ComponentInfoSource {

  private static final String TAG = ComponentInfoPm.class.getSimpleName();
  private final Context context;

  ComponentInfoPm(@NonNull Context context) {
    this.context = context;
  }

  @Override
  public ArrayList<AppModel> getInstalledApps(HashSet<String> filter) {
    final PackageManager pm = context.getPackageManager();
    final List<ApplicationInfo> appInfos =
        pm.getInstalledApplications(PackageManager.GET_META_DATA);

    try {
      Collections.sort(appInfos, new NameComparator(pm));
    } catch (Throwable t) {
      t.printStackTrace();
    }

    final ArrayList<AppModel> apps = new ArrayList<>(appInfos.size());
    int filtered = 0;

    for (ApplicationInfo ai : appInfos) {
      final Intent launchIntent = pm.getLaunchIntentForPackage(ai.packageName);
      if (launchIntent == null) {
        continue;
      }

      String launchStr = launchIntent.toString();
      launchStr = launchStr.substring(launchStr.indexOf("cmp=") + "cmp=".length());
      launchStr = launchStr.substring(0, launchStr.length() - 2);

      final String[] splitCode = launchStr.split("/");
      if (splitCode[1].startsWith(".")) {
        launchStr = splitCode[0] + "/" + splitCode[0] + splitCode[1];
      }

      if (filter.contains(launchStr)) {
        filtered++;
        log(TAG, "Filtered %s", launchStr);
        continue;
      }

      final String name = ai.loadLabel(pm).toString();
      apps.add(AppModel.create(name, launchStr, ai.packageName));
    }

    log(TAG, "Loaded %d total app(s), filtered out %d app(s).", apps.size(), filtered);
    return apps;
  }

  private static class NameComparator implements Comparator<ApplicationInfo> {

    private PackageManager packageManager;

    NameComparator(PackageManager pm) {
      packageManager = pm;
    }

    @Override
    public int compare(ApplicationInfo aa, ApplicationInfo ab) {
      CharSequence sa = packageManager.getApplicationLabel(aa);
      if (sa == null) {
        sa = aa.packageName;
      }
      CharSequence sb = packageManager.getApplicationLabel(ab);
      if (sb == null) {
        sb = ab.packageName;
      }
      return sa.toString().compareTo(sb.toString());
    }
  }
}
