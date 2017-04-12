package com.afollestad.iconrequest;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SendResult {

  static SendResult create(int sentCount, boolean usedPolarRm) {
    return new AutoValue_SendResult(sentCount, usedPolarRm, null);
  }

  static SendResult create(Exception e) {
    return new AutoValue_SendResult(0, false, e);
  }

  public abstract int sentCount();

  public abstract boolean usedPolarRm();

  public boolean success() {
    return error() != null;
  }

  @Nullable
  public abstract Exception error();
}
