package com.afollestad.iconrequest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App implements Serializable {

    private String mCode;
    private String mPkg;

    private transient Drawable mIcon;
    private transient CharSequence mName;

    public App() {
    }

    App(String code, String pkg) {
        mCode = code;
        mPkg = pkg;
    }

    public Drawable getIcon(Context context) {
        if (mIcon == null) {
            final ApplicationInfo ai = getAppInfo(context);
            if (ai != null)
                mIcon = ai.loadIcon(context.getPackageManager());
        }
        return mIcon;
    }

    public CharSequence getName(Context context) {
        if (mName == null) {
            final ApplicationInfo ai = getAppInfo(context);
            if (ai != null)
                mName = ai.loadLabel(context.getPackageManager());
            else mName = "Unknown";
        }
        return mName;
    }

    public String getCode() {
        return mCode;
    }

    public String getPackage() {
        return mPkg;
    }

    @Nullable
    public ApplicationInfo getAppInfo(Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(mPkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return mCode;
    }
}