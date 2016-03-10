package com.afollestad.iconrequest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.afollestad.iconrequest.glide.AppIconLoader;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public class App implements Serializable {

    private String mName;
    private String mCode;
    private String mPkg;
    private boolean mRequested;

    private transient Drawable mIcon;

    public App() {
    }

    App(String name, String code, String pkg, boolean requested) {
        mName = name;
        mCode = code;
        mPkg = pkg;
        mRequested = requested;
    }

    public Drawable getIcon(Context context) {
        if (mIcon == null) {
            final ApplicationInfo ai = getAppInfo(context);
            if (ai != null)
                mIcon = ai.loadIcon(context.getPackageManager());
        }
        return mIcon;
    }

    public void loadIcon(ImageView into) {
        if (IRUtils.inClassPath("com.bumptech.glide.load.model.ModelLoader")) {
            AppIconLoader.display(into, this);
        } else {
            into.setImageDrawable(getIcon(into.getContext()));
        }
    }

    public String getName() {
        return mName;
    }

    public String getCode() {
        return mCode;
    }

    public String getPackage() {
        return mPkg;
    }

    public boolean isRequested() {
        return mRequested;
    }

    public void setRequested(boolean requested) {
        this.mRequested = requested;
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

    @Override
    public boolean equals(Object o) {
        return o instanceof App && ((App) o).getCode().equals(getCode());
    }
}