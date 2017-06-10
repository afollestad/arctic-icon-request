package com.afollestad.iconrequestsample;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentActivity;
import com.afollestad.assent.AssentCallback;
import com.afollestad.assent.PermissionResultSet;
import com.afollestad.iconrequest.AppModel;
import com.afollestad.iconrequest.LoadResult;
import com.afollestad.iconrequest.PolarConfig;
import com.afollestad.iconrequest.PolarRequest;
import com.afollestad.iconrequest.SendResult;
import com.afollestad.materialdialogs.MaterialDialog;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.io.File;
import java.util.List;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MainActivity extends AssentActivity implements Toolbar.OnMenuItemClickListener {

  @BindView(R.id.rootView)
  View rootView;

  @BindView(R.id.progress)
  MaterialProgressBar progressView;

  @BindView(R.id.fab)
  FloatingActionButton fabView;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.list)
  RecyclerView listView;

  private PolarRequest request;
  private Unbinder unbinder;
  private MainAdapter adapter;
  private MaterialDialog dialog;
  private CompositeDisposable subs;

  @OnClick(R.id.fab)
  public void onClickFab() {
    if (!Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
      Assent.requestPermissions(
          new AssentCallback() {
            @Override
            public void onPermissionResult(PermissionResultSet results) {
              if (results.allPermissionsGranted()) {
                request.send().subscribe();
              } else {
                Snackbar.make(rootView, R.string.permission_denied, Snackbar.LENGTH_LONG).show();
              }
            }
          },
          69,
          Assent.WRITE_EXTERNAL_STORAGE);
      return;
    }
    request.send().subscribe();
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    unbinder = ButterKnife.bind(this);

    subs = new CompositeDisposable();

    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(this);
    fabView.hide();

    adapter = new MainAdapter();
    adapter.setListener(
        new MainAdapter.SelectionListener() {
          @Override
          public void onSelection(int index, AppModel app) {
            request.toggleSelection(app);
          }
        });

    GridLayoutManager lm =
        new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width));
    listView.setLayoutManager(lm);
    listView.setAdapter(adapter);

    PolarConfig config =
        PolarConfig.create(this).emailRecipient("fake-email@helloworld.com").build();
    request =
        PolarRequest.make(this, savedInstanceState)
            .config(config)
            .uriTransformer(
                new Function<Uri, Uri>() {
                  @Override
                  public Uri apply(Uri uri) {
                    return FileProvider.getUriForFile(
                        MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileProvider",
                        new File(uri.getPath()));
                  }
                });

    subs.add(
        request
            .loading()
            .subscribe(
                new Consumer<Boolean>() {
                  @Override
                  public void accept(Boolean isLoading) {
                    progressView.setVisibility(isLoading ? VISIBLE : GONE);
                  }
                }));
    subs.add(
        request
            .loaded()
            .subscribe(
                new Consumer<LoadResult>() {
                  @Override
                  public void accept(LoadResult loadResult) {
                    if (!loadResult.success()) {
                      adapter.setAppsList(null);
                      loadResult.error().printStackTrace();
                      Snackbar.make(rootView, loadResult.error().getMessage(), Snackbar.LENGTH_LONG)
                          .show();
                      return;
                    }
                    adapter.setAppsList(loadResult.apps());
                    invalidateToolbar();
                  }
                }));
    subs.add(
        request
            .selectionChange()
            .subscribe(
                new Consumer<AppModel>() {
                  @Override
                  public void accept(AppModel appModel) {
                    adapter.update(appModel);
                    invalidateToolbar();
                  }
                }));
    subs.add(
        request
            .sending()
            .subscribe(
                new Consumer<Boolean>() {
                  @Override
                  public void accept(Boolean isSending) {
                    if (isSending) {
                      dialog =
                          new MaterialDialog.Builder(MainActivity.this)
                              .content(R.string.preparing_your_request)
                              .progress(true, -1)
                              .cancelable(false)
                              .canceledOnTouchOutside(false)
                              .show();
                    } else if (dialog != null) {
                      dialog.dismiss();
                    }
                  }
                }));
    subs.add(
        request
            .sent()
            .subscribe(
                new Consumer<SendResult>() {
                  @Override
                  public void accept(SendResult sendResult) {
                    if (!sendResult.success()) {
                      sendResult.error().printStackTrace();
                      Snackbar.make(rootView, sendResult.error().getMessage(), Snackbar.LENGTH_LONG)
                          .show();
                    } else {
                      Snackbar.make(rootView, R.string.request_sent, Snackbar.LENGTH_SHORT).show();
                    }
                  }
                }));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (request.getLoadedApps().isEmpty()) {
      request.load().subscribe();
    }
  }

  private void invalidateToolbar() {
    subs.add(
        request
            .getSelectedApps()
            .subscribe(
                new Consumer<List<AppModel>>() {
                  @Override
                  public void accept(@NonNull List<AppModel> appModels) throws Exception {
                    int selectedCount = appModels.size();
                    if (selectedCount == 0) {
                      fabView.hide();
                      toolbar.setTitle(R.string.app_name);
                      toolbar
                          .getMenu()
                          .findItem(R.id.selectAllNone)
                          .setIcon(R.drawable.ic_action_selectall);
                    } else {
                      fabView.show();
                      toolbar.setTitle(getString(R.string.app_name_x, selectedCount));
                      toolbar
                          .getMenu()
                          .findItem(R.id.selectAllNone)
                          .setIcon(R.drawable.ic_action_selectall);
                    }
                  }
                }));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    request.saveInstance(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbinder.unbind();
    request = null;
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (dialog != null) {
      dialog.dismiss();
      dialog = null;
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.selectAllNone) {
      if (!request.getSelectedApps().blockingGet().isEmpty()) {
        request.deselectAll();
        item.setIcon(R.drawable.ic_action_selectall);
      } else {
        request.selectAll();
        item.setIcon(R.drawable.ic_action_selectnone);
      }
      return true;
    }
    return false;
  }
}
