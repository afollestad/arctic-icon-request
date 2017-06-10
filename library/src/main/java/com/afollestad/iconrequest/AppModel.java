package com.afollestad.iconrequest;

import static com.afollestad.iconrequest.IRUtils.inClassPath;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.afollestad.iconrequest.glide.AppIconLoader;
import com.google.auto.value.AutoValue;
import java.io.Serializable;

/** @author Aidan Follestad (afollestad) */
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

  public void loadIcon(ImageView into) {
    if (inClassPath("com.bumptech.glide.load.model.ModelLoader")) {
      AppIconLoader.display(into, this);
    } else {
      into.setImageDrawable(getIcon(into.getContext()));
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
