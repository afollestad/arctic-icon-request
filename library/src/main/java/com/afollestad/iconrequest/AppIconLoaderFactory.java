package com.afollestad.iconrequest;

import android.content.Context;
import android.support.annotation.NonNull;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.io.InputStream;

public class AppIconLoaderFactory implements ModelLoaderFactory<AppModel, InputStream> {

  private Context context;

  public AppIconLoaderFactory(Context context) {
    this.context = context;
  }

  @NonNull
  @Override
  public ModelLoader<AppModel, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
    return new AppIconLoader(context);
  }

  @Override
  public void teardown() {
    this.context = null;
  }
}
