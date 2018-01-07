package com.afollestad.iconrequest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;
import java.io.InputStream;

/** @author Aidan Follestad (afollestad) */
class AppIconLoader implements ModelLoader<AppModel, InputStream> {

  private final Context context;

  AppIconLoader(Context context) {
    this.context = context;
  }

  @Nullable
  @Override
  public LoadData<InputStream> buildLoadData(
      @NonNull AppModel appModel, int width, int height, @NonNull Options options) {
    return new LoadData<>(new ObjectKey(appModel.pkg()), new AppIconDataFetcher(context, appModel));
  }

  @Override
  public boolean handles(@NonNull AppModel appModel) {
    return true;
  }

  private class AppIconDataFetcher implements DataFetcher<InputStream> {

    private Context context;
    private AppModel model;

    AppIconDataFetcher(Context context, AppModel model) {
      this.context = context;
      this.model = model;
    }

    @Override
    public void loadData(
        @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
      try {
        callback.onDataReady(model.getIconStream(context));
      } catch (Exception e) {
        callback.onLoadFailed(e);
      }
    }

    @Override
    public void cleanup() {
      this.context = null;
      this.model = null;
    }

    @Override
    public void cancel() {}

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return DataSource.REMOTE;
    }
  }
}
