package com.afollestad.iconrequest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
@AutoValue
public abstract class AppModel implements Parcelable, Serializable {

  static AppModel create(String name, String code, String pkg) {
    return new AutoValue_AppModel(name, code, pkg, false, false);
  }

  public abstract String name();

  public abstract String code();

  public abstract String pkg();

  public abstract boolean requested();

  public abstract boolean selected();

  public abstract AppModel withSelected(boolean selected);

  public abstract AppModel withRequested(boolean requested);

  public abstract AppModel withSelectedAndRequested(boolean selected, boolean requested);

  public Drawable getIcon(Context context) {
    final ApplicationInfo ai = getAppInfo(context);
    if (ai == null) {
      return null;
    }
    return ai.loadIcon(context.getPackageManager());
  }

  public InputStream getIconStream(Context context) {
    Drawable drawable = getIcon(context);
    ByteArrayOutputStream os = null;
    try {
      final Bitmap bmp =
          Bitmap.createBitmap(
              drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
      final Canvas canvas = new Canvas(bmp);
      drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
      drawable.draw(canvas);
      os = new ByteArrayOutputStream(bmp.getByteCount());
      bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
      return new ByteArrayInputStream(os.toByteArray());
    } finally {
      FileUtil.closeQuietly(os);
    }
  }

  @Nullable
  private ApplicationInfo getAppInfo(Context context) {
    try {
      return context.getPackageManager().getApplicationInfo(pkg(), 0);
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }
}
