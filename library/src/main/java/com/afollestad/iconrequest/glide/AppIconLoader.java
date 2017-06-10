package com.afollestad.iconrequest.glide;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.afollestad.iconrequest.AppModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;

/** @author Aidan Follestad (afollestad) */
public class AppIconLoader implements ModelLoader<AppModel, AppModel> {

  public static void display(ImageView imageView, AppModel app) {
    Glide.with(imageView.getContext())
        .using(new AppIconLoader(), AppModel.class)
        .from(AppModel.class)
        .as(Drawable.class)
        .decoder(new ApplicationIconDecoder(imageView.getContext(), app.pkg()))
        .diskCacheStrategy(
            DiskCacheStrategy.NONE) // cannot disk cache ApplicationInfo, nor Drawables
        .load(app)
        .into(imageView);
  }

  @Override
  public DataFetcher<AppModel> getResourceFetcher(final AppModel model, int width, int height) {
    return new DataFetcher<AppModel>() {
      @Override
      public AppModel loadData(Priority priority) throws Exception {
        return model;
      }

      @Override
      public void cleanup() {}

      @Override
      public String getId() {
        return "AppIconLoader_" + model.pkg();
      }

      @Override
      public void cancel() {}
    };
  }
}
