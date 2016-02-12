package com.afollestad.iconrequest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
class ComponentInfoUtil {

    public static ArrayList<App> getInstalledApps(final Context context, final HashSet<String> filter,
                                                  final AppsLoadCallback cb, final Handler handler) {
        final PackageManager pm = context.getPackageManager();
        final List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(appInfos, new ApplicationInfo.DisplayNameComparator(pm));
        final ArrayList<App> apps = new ArrayList<>();

        int loaded = 0;
        for (ApplicationInfo ai : appInfos) {
            final Intent launchIntent = pm.getLaunchIntentForPackage(ai.packageName);
            if (launchIntent == null)
                continue;

            String launchStr = launchIntent.toString();
            launchStr = launchStr.substring(launchStr.indexOf("cmp=") + "cmp=".length());
            launchStr = launchStr.substring(0, launchStr.length() - 2);

            final String[] splitCode = launchStr.split("/");
            if (splitCode[1].startsWith("."))
                launchStr = splitCode[0] + "/" + splitCode[0] + splitCode[1];

            if (filter.contains(launchStr))
                continue;
            apps.add(new App(launchStr, ai.packageName));

            loaded++;
            final int percent = (loaded / appInfos.size()) * 100;
            if (cb != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onAppsLoadProgress(percent);
                    }
                });
            }
        }

        return apps;
    }

    private ComponentInfoUtil() {
    }
}