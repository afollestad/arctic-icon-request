package com.afollestad.iconrequest.glide;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.afollestad.iconrequest.AppModel;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.util.Util;
import java.io.IOException;

/** @author Aidan Follestad (afollestad) */
class ApplicationIconDecoder implements ResourceDecoder<AppModel, Drawable> {

  private final Context context;
  private final String pkg;

  ApplicationIconDecoder(Context context, String pkg) {
    this.context = context;
    this.pkg = pkg;
  }

  @Override
  public Resource<Drawable> decode(AppModel source, int width, int height) throws IOException {
    final Drawable icon;
    try {
      icon = context.getPackageManager().getApplicationIcon(source.pkg());
    } catch (PackageManager.NameNotFoundException e) {
      throw new IOException("Unable to load app icon for " + source.pkg());
    }
    return new DrawableResource<Drawable>(icon) {
      @Override
      public int getSize() {
        if (drawable instanceof BitmapDrawable) {
          return Util.getBitmapByteSize(((BitmapDrawable) drawable).getBitmap());
        } else {
          return 1;
        }
      }

      @Override
      public void recycle() {}
    };
  }

  @Override
  public String getId() {
    return "ApplicationIconDecoder_" + pkg;
  }
}
