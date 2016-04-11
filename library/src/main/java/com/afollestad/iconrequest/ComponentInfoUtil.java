package com.afollestad.iconrequest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
class ComponentInfoUtil {

    private static class NameComparator implements Comparator<ApplicationInfo> {

        private PackageManager mPM;

        public NameComparator(PackageManager pm) {
            mPM = pm;
        }

        @Override
        public int compare(ApplicationInfo aa, ApplicationInfo ab) {
            CharSequence sa = mPM.getApplicationLabel(aa);
            if (sa == null) {
                sa = aa.packageName;
            }
            CharSequence sb = mPM.getApplicationLabel(ab);
            if (sb == null) {
                sb = ab.packageName;
            }
            return sa.toString().compareTo(sb.toString());
        }
    }

    public static ArrayList<App> getInstalledApps(final Context context,
                                                  final HashSet<String> filter,
                                                  final AppsLoadCallback cb,
                                                  final Handler handler) {
        final PackageManager pm = context.getPackageManager();
        final List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        try {
            Collections.sort(appInfos, new NameComparator(pm));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        final ArrayList<App> apps = new ArrayList<>();

        int loaded = 0;
        int filtered = 0;
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

            if (filter.contains(launchStr)) {
                filtered++;
                IRLog.log("IconRequestApps", "Filtered %s", launchStr);
                continue;
            }

//            IRLog.log("IconRequestApps", "Loaded %s", launchStr);
            final String name = ai.loadLabel(pm).toString();
            apps.add(new App(name, launchStr, ai.packageName, false));

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

        IRLog.log("IconRequestApps", "Loaded %d total app(s), filtered out %d app(s).", apps.size(), filtered);
        return apps;
    }

    private ComponentInfoUtil() {
    }
}