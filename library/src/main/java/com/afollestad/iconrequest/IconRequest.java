package com.afollestad.iconrequest;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/**
 * @author Aidan Follestad (afollestad)
 */
public class IconRequest {

    private Builder mBuilder;
    private ArrayList<App> mApps;
    private ArrayList<App> mSelectedApps;
    private transient Handler mHandler;

    private static IconRequest mRequest;

    private IconRequest() {
        mSelectedApps = new ArrayList<>();
    }

    private IconRequest(Builder builder) {
        this();
        mBuilder = builder;
        mRequest = this;
    }

    public static class Builder implements Serializable {

        protected transient Context mContext;
        protected File mSaveDir = null;
        protected String mFilterName = "appfilter.xml";
        protected String mEmail = null;
        protected String mSubject = "Icon Request";
        protected String mHeader = "These apps aren't themed on my device, theme them please!";
        protected String mFooter = null;
        protected boolean mIncludeDeviceInfo = true;
        protected transient AppsLoadCallback mLoadCallback;
        protected transient RequestSendCallback mSendCallback;
        protected transient AppsSelectionListener mSelectionCallback;

        public Builder() {
        }

        public Builder(Context context) {
            mContext = context;
            mSaveDir = new File(Environment.getExternalStorageDirectory(), "IconRequest");
            FileUtil.wipe(mSaveDir);
        }

        public Builder filterName(@NonNull String filterName) {
            mFilterName = filterName;
            return this;
        }

        public Builder filterOff() {
            mFilterName = null;
            return this;
        }

        public Builder saveDir(@NonNull File file) {
            mSaveDir = file;
            return this;
        }

        public Builder toEmail(@NonNull String email) {
            mEmail = email;
            return this;
        }

        public Builder withSubject(@Nullable String subject, @Nullable Object... args) {
            if (args != null && subject != null)
                subject = String.format(subject, args);
            mSubject = subject;
            return this;
        }

        public Builder withHeader(@Nullable String header, @Nullable Object... args) {
            if (args != null && header != null)
                header = String.format(header, args);
            mHeader = header;
            return this;
        }

        public Builder withFooter(@Nullable String footer, @Nullable Object... args) {
            if (args != null && footer != null)
                footer = String.format(footer, args);
            mFooter = footer;
            return this;
        }

        public Builder includeDeviceInfo(boolean include) {
            mIncludeDeviceInfo = include;
            return this;
        }

        public Builder loadCallback(AppsLoadCallback cb) {
            mLoadCallback = cb;
            return this;
        }

        public Builder sendCallback(RequestSendCallback cb) {
            mSendCallback = cb;
            return this;
        }

        public Builder selectionCallback(AppsSelectionListener cb) {
            mSelectionCallback = cb;
            return this;
        }

        public IconRequest build() {
            return new IconRequest(this);
        }
    }

    public static Builder start(Context context) {
        return new Builder(context);
    }

    public static IconRequest get() {
        return mRequest;
    }

    public HashSet<String> loadFilterApps() {
        final HashSet<String> defined = new HashSet<>();
        if (IRUtils.isEmpty(mBuilder.mFilterName))
            return defined;

        InputStream is;
        try {
            final AssetManager am = mBuilder.mContext.getAssets();
            is = am.open(mBuilder.mFilterName);
        } catch (Throwable e) {
            e.printStackTrace();
            if (mBuilder.mLoadCallback != null)
                mBuilder.mLoadCallback.onAppsLoaded(null, new Exception("Failed to open your filter: " + e.getLocalizedMessage(), e));
            return defined;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            final String startStr = "component=\"ComponentInfo{";
            final String endStr = "}\"";
            String line;
            while ((line = reader.readLine()) != null) {
                int start = line.indexOf(startStr);
                if (start == -1) continue;
                start += startStr.length();
                int end = line.indexOf(endStr);
                defined.add(line.substring(start, end));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mBuilder.mLoadCallback != null)
                mBuilder.mLoadCallback.onAppsLoaded(null, new Exception("Failed to read your filter: " + e.getMessage(), e));
        } finally {
            FileUtil.closeQuietely(reader);
            FileUtil.closeQuietely(is);
        }

        return defined;
    }

    public void loadApps() {
        if (mBuilder.mLoadCallback == null)
            throw new IllegalStateException("No load callback has been set.");
        if (mHandler == null)
            mHandler = new Handler();
        if (mBuilder.mLoadCallback != null)
            mBuilder.mLoadCallback.onLoadingFilter();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final HashSet<String> filter = loadFilterApps();
                mApps = ComponentInfoUtil.getInstalledApps(mBuilder.mContext,
                        filter, mBuilder.mLoadCallback, mHandler);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBuilder.mLoadCallback.onAppsLoaded(mApps, null);
                    }
                });
            }
        }).start();
    }

    @SuppressWarnings("MalformedFormatString")
    private String getBody() {
        StringBuilder sb = new StringBuilder();
        if (!IRUtils.isEmpty(mBuilder.mHeader)) {
            sb.append(mBuilder.mHeader.replace("\n", "<br/>"));
            sb.append("<br/><br/>");
        }

        for (int i = 0; i < mApps.size(); i++) {
            if (i > 0) sb.append("<br/><br/>");
            final App app = mApps.get(i);
            sb.append(String.format("Name: <b>%s</b><br/>", app.getName(mBuilder.mContext)));
            sb.append(String.format("Code: <b>%s</b><br/>", app.getCode()));
            sb.append(String.format("Link: https://play.google.com/store/apps/details?id=%s<br/>", app.getPackage()));
        }

        if (mBuilder.mIncludeDeviceInfo) {
            sb.append(String.format(Locale.getDefault(),
                    "<br/><br/>OS: %s %s<br/>Device: %s %s (%s)",
                    Build.VERSION.RELEASE, IRUtils.getOSVersionName(Build.VERSION.SDK_INT),
                    Build.MANUFACTURER, Build.MODEL, Build.PRODUCT));
            if (mBuilder.mFooter != null) {
                sb.append("<br/>");
                sb.append(mBuilder.mFooter.replace("\n", "<br/>"));
            }
        } else {
            sb.append("<br/><br/>");
            sb.append(mBuilder.mFooter.replace("\n", "<br/>"));
        }
        return sb.toString();
    }

    public boolean selectApp(App app) {
        if (!mSelectedApps.contains(app)) {
            mSelectedApps.add(app);
            if (mBuilder.mSelectionCallback != null)
                mBuilder.mSelectionCallback.onAppSelectionChanged(mSelectedApps.size());
            return true;
        }
        return false;
    }

    public boolean unselectApp(App app) {
        final boolean result = mSelectedApps.remove(app);
        if (result && mBuilder.mSelectionCallback != null)
            mBuilder.mSelectionCallback.onAppSelectionChanged(mSelectedApps.size());
        return result;
    }

    public boolean toggleAppSelected(App app) {
        final boolean result;
        if (isAppSelected(app))
            result = unselectApp(app);
        else result = selectApp(app);
        return result;
    }

    public boolean isAppSelected(App app) {
        return mSelectedApps.contains(app);
    }

    public void selectAllApps() {
        if (mSelectedApps.size() == 0) {
            mSelectedApps.addAll(mApps);
            if (mBuilder.mSelectionCallback != null)
                mBuilder.mSelectionCallback.onAppSelectionChanged(mSelectedApps.size());
        } else {
            boolean changed = false;
            for (App app : mApps) {
                if (!mSelectedApps.contains(app)) {
                    changed = true;
                    mSelectedApps.add(app);
                }
            }
            if (changed && mBuilder.mSelectionCallback != null)
                mBuilder.mSelectionCallback.onAppSelectionChanged(mSelectedApps.size());
        }
    }

    public void unselectAllApps() {
        if (mSelectedApps.size() == 0) return;
        mSelectedApps.clear();
        if (mBuilder.mSelectionCallback != null)
            mBuilder.mSelectionCallback.onAppSelectionChanged(0);
    }

    public ArrayList<App> getApps() {
        return mApps;
    }

    public ArrayList<App> getSelectedApps() {
        if (mSelectedApps == null)
            mSelectedApps = new ArrayList<>();
        return mSelectedApps;
    }

    public void send() {
        if (IRUtils.isEmpty(mBuilder.mEmail))
            throw new IllegalStateException("The email cannot be empty.");
        else if (IRUtils.isEmpty(mBuilder.mSubject))
            mBuilder.mSubject = "Icon Request";

        if (mBuilder.mSendCallback != null)
            mBuilder.mSendCallback.onRequestPreparing();
        if (mHandler == null)
            mHandler = new Handler();

        new Thread(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                final ArrayList<File> filesToZip = new ArrayList<>();
                final ArrayList<App> apps = mSelectedApps != null ? mSelectedApps : mApps;
                mBuilder.mSaveDir.mkdirs();

                // Save app icons
                for (App app : apps) {
                    final Drawable drawable = app.getIcon(mBuilder.mContext);
                    if (!(drawable instanceof BitmapDrawable)) continue;
                    final BitmapDrawable bDrawable = (BitmapDrawable) drawable;
                    final Bitmap icon = bDrawable.getBitmap();
                    final File file = new File(mBuilder.mSaveDir,
                            String.format("%s.png", app.getCode().replace("/", "_")));
                    filesToZip.add(file);
                    try {
                        FileUtil.writeIcon(file, icon);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        if (mBuilder.mSendCallback != null) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mBuilder.mSendCallback.onRequestError(
                                            new Exception("Failed to save an icon: " + e.getMessage(), e));
                                }
                            });
                        }
                        return;
                    }
                }

                // Create appfilter.xml
                final StringBuilder sb = new StringBuilder("<resources>\n" +
                        "    <iconback img1=\"iconback\" />\n" +
                        "    <iconmask img1=\"iconmask\" />\n" +
                        "    <iconupon img1=\"iconupon\" />\n" +
                        "    <scale factor=\"1.0\" />");
                for (App app : mApps) {
                    final String name = app.getName(mBuilder.mContext).toString();
                    sb.append("\n\n    <!-- ");
                    sb.append(name);
                    sb.append(" -->\n");
                    sb.append(String.format("    <item\n" +
                                    "        component=\"ComponentInfo{%s}\"\n" +
                                    "        drawable=\"%s\" />",
                            app.getCode(), IRUtils.drawableName(name)));
                }
                sb.append("\n\n</resources>");

                final File newAppFilter = new File(mBuilder.mSaveDir, "appfilter.xml");
                filesToZip.add(newAppFilter);
                try {
                    FileUtil.writeAll(newAppFilter, sb.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (mBuilder.mSendCallback != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mBuilder.mSendCallback.onRequestError(new Exception(
                                        "Failed to write your request appfilter.xml file: " + e.getMessage(), e));
                            }
                        });
                    }
                    return;
                }

                // Zip everything into an archive
                final SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                final File zipFile = new File(mBuilder.mSaveDir,
                        String.format("IconRequest-%s.zip", df.format(new Date())));
                try {
                    ZipUtil.zip(zipFile, filesToZip.toArray(new File[filesToZip.size()]));
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (mBuilder.mSendCallback != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mBuilder.mSendCallback.onRequestError(new Exception(
                                        "Failed to create the request ZIP file: " + e.getMessage(), e));
                            }
                        });
                    }
                    return;
                }

                // Cleanup files
                final File[] files = mBuilder.mSaveDir.listFiles();
                for (File fi : files) {
                    if (!fi.isDirectory() && (fi.getName().endsWith(".png") || fi.getName().endsWith(".xml")))
                        fi.delete();
                }

                // Send email intent
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        final Uri zipUri = Uri.fromFile(zipFile);
                        final Intent emailIntent = new Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_EMAIL, new String[]{mBuilder.mEmail})
                                .putExtra(Intent.EXTRA_SUBJECT, mBuilder.mSubject)
                                .putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getBody()))
                                .putExtra(Intent.EXTRA_STREAM, zipUri)
                                .setDataAndType(zipUri, "application/zip");
                        mBuilder.mContext.startActivity(Intent.createChooser(
                                emailIntent, mBuilder.mContext.getString(R.string.send_using)));
                        if (mBuilder.mSendCallback != null)
                            mBuilder.mSendCallback.onRequestSent();
                    }
                });
            }
        }).start();
    }

    public static void saveInstanceState(Bundle outState) {
        if (mRequest == null || outState == null) return;
        outState.putSerializable("builder", mRequest.mBuilder);
        outState.putSerializable("apps", mRequest.mApps);
        outState.putSerializable("selected_apps", mRequest.mSelectedApps);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static IconRequest restoreInstanceState(Context context, Bundle inState,
                                                   AppsLoadCallback loadCb,
                                                   RequestSendCallback sendCb,
                                                   AppsSelectionListener selectionCb) {
        if (inState == null)
            return null;
        mRequest = new IconRequest();
        if (inState.containsKey("builder")) {
            mRequest.mBuilder = (Builder) inState.getSerializable("builder");
            if (mRequest.mBuilder != null) {
                mRequest.mBuilder.mContext = context;
                mRequest.mBuilder.mLoadCallback = loadCb;
                mRequest.mBuilder.mSendCallback = sendCb;
                mRequest.mBuilder.mSelectionCallback = selectionCb;
            }
        }
        if (inState.containsKey("apps"))
            mRequest.mApps = (ArrayList<App>) inState.getSerializable("apps");
        if (inState.containsKey("selected_apps"))
            mRequest.mSelectedApps = (ArrayList<App>) inState.getSerializable("selected_apps");
        if (mRequest.mApps == null)
            mRequest.mApps = new ArrayList<>();
        if (mRequest.mSelectedApps == null)
            mRequest.mSelectedApps = new ArrayList<>();
        else if (mRequest.mSelectedApps.size() > 0 && selectionCb != null)
            selectionCb.onAppSelectionChanged(mRequest.mSelectedApps.size());
        return mRequest;
    }

    public static void cleanup() {
        if (mRequest == null) return;
        mRequest.mBuilder.mContext = null;
        mRequest.mBuilder = null;
        mRequest.mHandler = null;
        mRequest.mApps.clear();
        mRequest.mApps = null;
        mRequest.mSelectedApps.clear();
        mRequest.mSelectedApps = null;
        mRequest = null;
    }
}