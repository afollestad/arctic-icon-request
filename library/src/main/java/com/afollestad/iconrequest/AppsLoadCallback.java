package com.afollestad.iconrequest;

import java.util.ArrayList;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface AppsLoadCallback {

    void onLoadingFilter();

    void onAppsLoaded(ArrayList<App> apps, Exception e);

    void onAppsLoadProgress(int percent);
}