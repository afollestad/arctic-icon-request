package com.afollestad.iconrequest;

import static com.afollestad.iconrequest.IRLog.log;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PolarRequest {

  private static final String TAG = PolarRequest.class.getSimpleName();
  private static final String KEY_CONFIG = "ir.config";
  private static final String KEY_FILTER = "ir.loadedFilter";
  private static final String KEY_APPS = "ir.loadedApps";

  private final PublishSubject<Boolean> loadingSubject;
  private final PublishSubject<LoadResult> loadedSubject;
  private final PublishSubject<Boolean> sendingSubject;
  private final PublishSubject<SendResult> sentSubject;
  private final PublishSubject<AppModel> selectionChangeSubject;

  private final AppFilterSource appFilterSource;
  private final ComponentInfoSource componentInfoSource;
  private final SendInteractor sendInteractor;
  PolarConfig config;
  Function<Uri, Uri> uriTransformer;
  private HashSet<String> loadedFilter;
  private List<AppModel> loadedApps;

  private PolarRequest(@NonNull Context context) {
    this.loadingSubject = PublishSubject.create();
    this.loadedSubject = PublishSubject.create();
    this.sendingSubject = PublishSubject.create();
    this.sentSubject = PublishSubject.create();
    this.selectionChangeSubject = PublishSubject.create();

    this.appFilterSource = new AppFilterAssets(context);
    this.componentInfoSource = new ComponentInfoPm(context);
    this.sendInteractor = new RealSendInteractor(context);

    this.loadedFilter = new HashSet<>(0);
    this.loadedApps = new ArrayList<>(0);
    this.config = PolarConfig.create(context).build();
    this.uriTransformer = uri -> uri;
  }

  @NonNull
  public static PolarRequest make(@NonNull Context context, @Nullable Bundle savedInstanceState) {
    return new PolarRequest(context).restoreInstance(context, savedInstanceState);
  }

  public PolarRequest config(@NonNull PolarConfig config) {
    this.config = config;
    return this;
  }

  private PolarRequest restoreInstance(
      @NonNull Context context, @Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return this;
    }
    config = savedInstanceState.getParcelable(KEY_CONFIG);
    if (config == null) {
      config = PolarConfig.create(context).build();
    }
    //noinspection unchecked
    loadedFilter = (HashSet<String>) savedInstanceState.getSerializable(KEY_FILTER);
    if (loadedFilter == null) {
      loadedFilter = new HashSet<>(0);
    }
    loadedApps = savedInstanceState.getParcelableArrayList(KEY_APPS);
    if (loadedApps == null) {
      loadedApps = new ArrayList<>(0);
    } else if (!loadedApps.isEmpty()) {
      log(TAG, "Got %d apps from restored instance state.", loadedApps.size());
      loadedSubject.onNext(LoadResult.create(loadedApps));
      loadingSubject.onNext(false);
    }
    return this;
  }

  public void saveInstance(@Nullable Bundle out) {
    if (out == null) {
      return;
    }
    out.putParcelable(KEY_CONFIG, config);
    out.putSerializable(KEY_FILTER, loadedFilter);
    out.putParcelableArrayList(KEY_APPS, (ArrayList<AppModel>) loadedApps);
  }

  @NonNull
  public PolarRequest uriTransformer(@NonNull Function<Uri, Uri> transformer) {
    this.uriTransformer = transformer;
    return this;
  }

  private static void transferStates(List<AppModel> from, final List<AppModel> to) {
    Observable.just(from)
        .flatMapIterable(appModels -> appModels)
        .filter(AppModel::selected)
        .forEach(
            appModel -> {
              for (int i = 0; i < to.size(); i++) {
                AppModel current = to.get(i);
                if (appModel.code().equals(current.code())) {
                  to.set(
                      i,
                      current.withSelectedAndRequested(appModel.selected(), appModel.requested()));
                  break;
                }
              }
            });
  }

  @NonNull
  public Observable<LoadResult> load() {
    return Observable.fromCallable(
            () -> {
              loadingSubject.onNext(true);
              try {
                loadedFilter =
                    appFilterSource.load(config.appFilterName(), config.errorOnInvalidDrawables());
              } catch (Exception e) {
                return LoadResult.create(e);
              }
              List<AppModel> newLoadedApps = componentInfoSource.getInstalledApps(loadedFilter);
              if (!loadedApps.isEmpty()) {
                transferStates(loadedApps, newLoadedApps);
              }
              loadedApps = newLoadedApps;
              return LoadResult.create(loadedApps);
            })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .doOnNext(
            loadResult -> {
              loadingSubject.onNext(false);
              loadedSubject.onNext(loadResult);
            });
  }

  @NonNull
  public HashSet<String> getLoadedFilter() {
    return loadedFilter;
  }

  @NonNull
  public List<AppModel> getLoadedApps() {
    return loadedApps;
  }

  @NonNull
  public Single<List<AppModel>> getSelectedApps() {
    return Observable.just(loadedApps)
        .flatMapIterable(appModels -> appModels)
        .filter(AppModel::selected)
        .toList();
  }

  @NonNull
  public Single<List<AppModel>> getRequestedApps() {
    return Observable.just(loadedApps)
        .flatMapIterable(appModels -> appModels)
        .filter(AppModel::requested)
        .toList();
  }

  public boolean isSelected(@NonNull AppModel app) {
    int index = loadedApps.indexOf(app);
    if (index == -1) {
      throw new IllegalArgumentException(
          "Unable to find app " + app.pkg() + " in this list of loaded apps!");
    }
    return loadedApps.get(index).selected();
  }

  @NonNull
  public PolarRequest select(@NonNull AppModel app) {
    int index = loadedApps.indexOf(app);
    if (index == -1) {
      throw new IllegalArgumentException(
          "Unable to find app " + app.pkg() + " in this list of loaded apps!");
    }
    app = loadedApps.get(index);
    if (app.selected()) {
      return this;
    }
    app = app.withSelected(true);
    loadedApps.set(index, app);
    selectionChangeSubject.onNext(app);
    return this;
  }

  @NonNull
  public PolarRequest deselect(@NonNull AppModel app) {
    int index = loadedApps.indexOf(app);
    if (index == -1) {
      throw new IllegalArgumentException(
          "Unable to find app " + app.pkg() + " in this list of loaded apps!");
    }
    app = loadedApps.get(index);
    if (!app.selected()) {
      return this;
    }
    app = app.withSelected(false);
    loadedApps.set(index, app);
    selectionChangeSubject.onNext(app);
    return this;
  }

  @NonNull
  public PolarRequest toggleSelection(@NonNull AppModel app) {
    int index = loadedApps.indexOf(app);
    if (index == -1) {
      throw new IllegalArgumentException(
          "Unable to find app " + app.pkg() + " in this list of loaded apps!");
    }
    app = loadedApps.get(index);
    // Toggle selection state
    app = app.withSelected(!app.selected());
    loadedApps.set(index, app);
    selectionChangeSubject.onNext(app);
    return this;
  }

  @NonNull
  public PolarRequest selectAll() {
    for (int i = 0; i < loadedApps.size(); i++) {
      AppModel app = loadedApps.get(i);
      if (app.selected()) {
        continue;
      }
      app = app.withSelected(true);
      loadedApps.set(i, app);
    }
    loadedSubject.onNext(LoadResult.create(loadedApps));
    return this;
  }

  @NonNull
  public PolarRequest deselectAll() {
    for (int i = 0; i < loadedApps.size(); i++) {
      AppModel app = loadedApps.get(i);
      if (!app.selected()) {
        continue;
      }
      app = app.withSelected(false);
      loadedApps.set(i, app);
    }
    loadedSubject.onNext(LoadResult.create(loadedApps));
    return this;
  }

  private void resetSelection() {
    for (int i = 0; i < loadedApps.size(); i++) {
      AppModel app = loadedApps.get(i);
      if (!app.selected()) {
        continue;
      }
      app = app.withSelected(false).withRequested(true);
      loadedApps.set(i, app);
    }
    loadedSubject.onNext(LoadResult.create(loadedApps));
  }

  @NonNull
  public Observable<SendResult> send() {

    return Observable.fromCallable(
            () -> {
              sendingSubject.onNext(true);
              try {
                List<AppModel> selectedApps = getSelectedApps().blockingGet();
                boolean remote = sendInteractor.send(selectedApps, PolarRequest.this);
                return SendResult.create(selectedApps.size(), remote);
              } catch (Exception e) {
                return SendResult.create(e);
              }
            })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .doOnNext(
            sendResult -> {
              resetSelection();
              sendingSubject.onNext(false);
              sentSubject.onNext(sendResult);
            });
  }

  @NonNull
  public Observable<Boolean> loading() {
    return loadingSubject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation());
  }

  @NonNull
  public Observable<LoadResult> loaded() {
    return loadedSubject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation());
  }

  @NonNull
  public Observable<Boolean> sending() {
    return sendingSubject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation());
  }

  @NonNull
  public Observable<SendResult> sent() {
    return sentSubject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation());
  }

  @NonNull
  public Observable<AppModel> selectionChange() {
    return selectionChangeSubject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation());
  }
}
