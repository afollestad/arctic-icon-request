package com.afollestad.iconrequest;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import static com.afollestad.iconrequest.FileUtil.wipe;

@SuppressWarnings("WeakerAccess")
@AutoValue
public abstract class PolarConfig implements Parcelable {

  public static Builder create(@NonNull Context context) {
    File defaultCache = new File(context.getExternalCacheDir(), "com.afollestad.polar");
    wipe(defaultCache);
    return new AutoValue_PolarConfig.Builder()
        .cacheFolder(defaultCache.getAbsolutePath())
        .appFilterName("appfilter.xml")
        .emailSubject("Icon Request")
        .emailHeader("These apps aren't themed on my device!")
        .errorOnInvalidDrawables(true)
        .includeDeviceInfo(true);
  }

  public abstract String cacheFolder();

  public abstract String appFilterName();

  @Nullable
  public abstract String emailRecipient();

  @Nullable
  public abstract String emailSubject();

  @Nullable
  public abstract String emailHeader();

  @Nullable
  public abstract String emailFooter();

  public abstract boolean includeDeviceInfo();

  public abstract boolean errorOnInvalidDrawables();

  @Nullable
  public abstract String apiKey();

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder cacheFolder(String folder);

    public abstract Builder appFilterName(String name);

    public abstract Builder emailRecipient(@Nullable String recpient);

    public abstract Builder emailSubject(@Nullable String subject);

    public abstract Builder emailHeader(@Nullable String header);

    public abstract Builder emailFooter(@Nullable String footer);

    public abstract Builder includeDeviceInfo(boolean include);

    public abstract Builder errorOnInvalidDrawables(boolean error);

    public abstract Builder apiKey(@Nullable String key);

    public abstract PolarConfig build();
  }
}
