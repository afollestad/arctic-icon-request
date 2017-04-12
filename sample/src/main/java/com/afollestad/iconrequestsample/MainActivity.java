package com.afollestad.iconrequestsample;

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
import android.widget.TextView;

import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentActivity;
import com.afollestad.iconrequest.PolarConfig;
import com.afollestad.iconrequest.PolarRequest;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AssentActivity implements Toolbar.OnMenuItemClickListener {

  @BindView(R.id.rootView)
  View rootView;
  @BindView(R.id.progress)
  TextView progressView;
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

  @OnClick(R.id.fab)
  public void onClickFab() {
    if (!Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
      Assent.requestPermissions(results -> {
        if (results.allPermissionsGranted()) {
          request.send().subscribe();
        } else {
          Snackbar.make(rootView,
              R.string.permission_denied,
              Snackbar.LENGTH_LONG).show();
        }
      }, 69, Assent.WRITE_EXTERNAL_STORAGE);
      return;
    }
    request.send().subscribe();
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    unbinder = ButterKnife.bind(this);

    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(this);
    fabView.hide();

    adapter = new MainAdapter();
    adapter.setListener((index, app) -> request.toggleSelection(app));

    GridLayoutManager lm = new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width));
    listView.setLayoutManager(lm);
    listView.setAdapter(adapter);

    PolarConfig config = PolarConfig.create(this)
        .emailRecipient("fake-email@helloworld.com")
        .build();
    request = PolarRequest.make(this, savedInstanceState)
        .config(config)
        .uriTransformer(uri -> FileProvider.getUriForFile(this,
            BuildConfig.APPLICATION_ID + ".fileProvider",
            new File(uri.getPath())));

    request.loading()
        .subscribe(isLoading -> progressView.setVisibility(isLoading ? VISIBLE : GONE));
    request.loaded()
        .subscribe(loadResult -> {
          if (!loadResult.success()) {
            adapter.setAppsList(null);
            loadResult.error().printStackTrace();
            Snackbar.make(rootView,
                loadResult.error().getMessage(),
                Snackbar.LENGTH_LONG).show();
            return;
          }
          adapter.setAppsList(loadResult.apps());
          invalidateToolbar();
        });
    request.selectionChange()
        .subscribe(appModel -> {
          adapter.update(appModel);
          invalidateToolbar();
        });
    request.sending()
        .subscribe(isSending -> {
          if (isSending) {
            dialog = new MaterialDialog.Builder(this)
                .content(R.string.preparing_your_request)
                .progress(true, -1)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();
          } else if (dialog != null) {
            dialog.dismiss();
          }
        });
    request.sent()
        .subscribe(sendResult -> {
          if (!sendResult.success()) {
            sendResult.error().printStackTrace();
            Snackbar.make(rootView,
                sendResult.error().getMessage(),
                Snackbar.LENGTH_LONG).show();
          } else {
            Snackbar.make(rootView, R.string.request_sent, Snackbar.LENGTH_SHORT).show();
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    request.load().subscribe();
  }

  private void invalidateToolbar() {
    int selectedCount = request.getSelectedApps().size();
    if (selectedCount == 0) {
      fabView.hide();
      toolbar.setTitle(R.string.app_name);
      toolbar.getMenu().findItem(R.id.selectAllNone)
          .setIcon(R.drawable.ic_action_selectall);
    } else {
      fabView.show();
      toolbar.setTitle(getString(R.string.app_name_x, selectedCount));
      toolbar.getMenu().findItem(R.id.selectAllNone)
          .setIcon(R.drawable.ic_action_selectall);
    }
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
      if (!request.getSelectedApps().isEmpty()) {
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