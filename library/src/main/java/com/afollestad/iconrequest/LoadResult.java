package com.afollestad.iconrequest;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class LoadResult implements Parcelable {

  static LoadResult create(List<AppModel> apps) {
    return new AutoValue_LoadResult(apps, null);
  }

  static LoadResult create(Exception error) {
    return new AutoValue_LoadResult(new ArrayList<>(0), error);
  }

  public boolean success() {
    return error() != null;
  }

  public abstract List<AppModel> apps();

  @Nullable
  public abstract Exception error();
}
