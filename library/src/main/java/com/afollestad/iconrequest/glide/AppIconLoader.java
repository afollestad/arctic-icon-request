package com.afollestad.iconrequest.glide;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.afollestad.iconrequest.App;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;

/**
 * @author Aidan Follestad (afollestad)
 */
public class AppIconLoader implements ModelLoader<App, App> {

    @Override
    public DataFetcher<App> getResourceFetcher(final App model, int width, int height) {
        return new DataFetcher<App>() {
            @Override
            public App loadData(Priority priority) throws Exception {
                return model;
            }

            @Override
            public void cleanup() {
            }

            @Override
            public String getId() {
                return "AppIconLoader_" + model.getPackage();
            }

            @Override
            public void cancel() {
            }
        };
    }

    public static void display(ImageView imageView, App app) {
        Glide.with(imageView.getContext())
                .using(new AppIconLoader(), App.class)
                .from(App.class)
                .as(Drawable.class)
                .decoder(new ApplicationIconDecoder(imageView.getContext(), app.getPackage()))
                .diskCacheStrategy(DiskCacheStrategy.NONE) // cannot disk cache ApplicationInfo, nor Drawables
                .load(app)
                .into(imageView);
    }

}