package com.afollestad.iconrequestsample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentActivity;
import com.afollestad.iconrequest.PolarRequest;
import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.schedulers.Schedulers;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    unbinder = ButterKnife.bind(this);

    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(this);

    fabView.hide();
    fabView.setOnClickListener(view -> {
      if (!Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
        Assent.requestPermissions(results -> {
          if (results.allPermissionsGranted()) {
            request.send()
                .subscribeOn(Schedulers.computation())
                .subscribe();
          } else {
            Snackbar.make(rootView,
                R.string.permission_denied,
                Snackbar.LENGTH_LONG).show();
          }
        }, 69, Assent.WRITE_EXTERNAL_STORAGE);
        return;
      }
      request.send()
          .subscribeOn(Schedulers.computation())
          .subscribe();
    });

    adapter = new MainAdapter();
    adapter.setListener((index, app) -> request.toggleSelection(app));

    GridLayoutManager lm = new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width));
    listView.setLayoutManager(lm);
    listView.setAdapter(adapter);

    request = PolarRequest.make(this, savedInstanceState);
    request.loading()
        .subscribe(isLoading -> progressView.setVisibility(isLoading ? VISIBLE : GONE));
    request.loaded()
        .subscribe(loadResult -> {
          if (!loadResult.success()) {
            adapter.setAppsList(null);
            dialog = new MaterialDialog.Builder(this)
                .title(R.string.error)
                .content(loadResult.error().getMessage())
                .positiveText(android.R.string.ok)
                .show();
            return;
          }
          adapter.setAppsList(loadResult.apps());
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
          if (dialog != null) {
            dialog.dismiss();
          }
          if (!sendResult.success()) {
            dialog = new MaterialDialog.Builder(this)
                .title(R.string.error)
                .content(sendResult.error().getMessage())
                .positiveText(android.R.string.ok)
                .show();
          } else {
            Snackbar.make(rootView, R.string.request_sent, Snackbar.LENGTH_SHORT).show();
          }
        });

    if (request.getLoadedApps().isEmpty()) {
      request.load()
          .subscribe();
    }
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