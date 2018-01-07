package com.afollestad.iconrequestsample;

import android.content.Context;
import android.support.annotation.NonNull;
import com.afollestad.iconrequest.AppIconLoaderFactory;
import com.afollestad.iconrequest.AppModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import java.io.InputStream;

@GlideModule
public class AppIconGlideModule extends AppGlideModule {

  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    super.registerComponents(context, glide, registry);
    registry.replace(AppModel.class, InputStream.class, new AppIconLoaderFactory(context));
  }
}
