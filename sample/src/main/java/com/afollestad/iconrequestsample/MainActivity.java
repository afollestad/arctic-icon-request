package com.afollestad.iconrequestsample;

import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.assent.Assent;
import com.afollestad.assent.AssentActivity;
import com.afollestad.assent.AssentCallback;
import com.afollestad.assent.PermissionResultSet;
import com.afollestad.iconrequest.App;
import com.afollestad.iconrequest.AppsLoadCallback;
import com.afollestad.iconrequest.AppsSelectionListener;
import com.afollestad.iconrequest.IconRequest;
import com.afollestad.iconrequest.RequestSendCallback;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AssentActivity implements AppsLoadCallback, RequestSendCallback, AppsSelectionListener, Toolbar.OnMenuItemClickListener {

    private TextView mProgress;
    private MainAdapter mAdapter;
    private FloatingActionButton mFab;
    private Toolbar mToolbar;

    private MaterialDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgress = (TextView) findViewById(R.id.progress);
        mFab = (FloatingActionButton) findViewById(R.id.fab);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.setOnMenuItemClickListener(this);

        mFab.hide();
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
                    Assent.requestPermissions(new AssentCallback() {
                        @Override
                        public void onPermissionResult(PermissionResultSet results) {
                            if (results.allPermissionsGranted())
                                IconRequest.get().send();
                            else
                                Toast.makeText(MainActivity.this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                        }
                    }, 69, Assent.WRITE_EXTERNAL_STORAGE);
                    return;
                }
                IconRequest.get().send();
            }
        });

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        GridLayoutManager lm = new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width));
        list.setLayoutManager(lm);

        mAdapter = new MainAdapter();
        list.setAdapter(mAdapter);

        if (savedInstanceState != null)
            IconRequest.restoreInstanceState(this, savedInstanceState, this, this, this);

        // Only load if it wasn't saved before a configuration change
        if (IconRequest.get() == null) {
            IconRequest request = IconRequest.start(this)
                    .withHeader("Hey, testing Icon Request!")
                    .withFooter("%s Version: %s", getString(R.string.app_name), BuildConfig.VERSION_NAME)
                    .withSubject("Icon Request - Just a Test")
                    .toEmail("fake-email@fake-website.com")
                    .saveDir(new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name)))
                    .includeDeviceInfo(true) // defaults to true anyways
                    .generateAppFilterXml(true) // defaults to true anyways
                    .generateAppFilterJson(true)
                    .loadCallback(this)
                    .sendCallback(this)
                    .selectionCallback(this)
                    .build();
            request.loadApps();
        }
    }

    @Override
    public void onLoadingFilter() {
        mProgress.setVisibility(View.VISIBLE);
        mProgress.setText(R.string.loading_filter);
    }

    @Override
    public void onAppsLoaded(ArrayList<App> apps, Exception error) {
        if (error != null) {
            mProgress.setVisibility(View.VISIBLE);
            mProgress.setText(error.getMessage());
            return;
        }
        mProgress.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAppsLoadProgress(int percent) {
        mProgress.setVisibility(View.VISIBLE);
        mProgress.setText(getString(R.string.loading_percent, percent));
    }

    @Override
    public void onRequestPreparing() {
        mDialog = new MaterialDialog.Builder(this)
                .content(R.string.preparing_your_request)
                .progress(true, -1)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();
    }

    @Override
    public void onRequestError(Exception e) {
        mDialog.dismiss();
        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestSent() {
        mDialog.dismiss();
        Toast.makeText(this, R.string.request_sent, Toast.LENGTH_SHORT).show();
        IconRequest.get().unselectAllApps();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAppSelectionChanged(int selectedCount) {
        if (selectedCount == 0) {
            mFab.hide();
            mToolbar.setTitle(R.string.app_name);
            mToolbar.getMenu().findItem(R.id.selectAllNone)
                    .setIcon(R.drawable.ic_action_selectall);
        } else {
            mFab.show();
            mToolbar.setTitle(getString(R.string.app_name_x, selectedCount));
            mToolbar.getMenu().findItem(R.id.selectAllNone)
                    .setIcon(R.drawable.ic_action_selectall);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (isFinishing())
            IconRequest.cleanup();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.selectAllNone) {
            IconRequest ir = IconRequest.get();
            if (ir.getSelectedApps().size() > 0) {
                ir.unselectAllApps();
                item.setIcon(R.drawable.ic_action_selectall);
                mAdapter.notifyDataSetChanged();
            } else {
                ir.selectAllApps();
                item.setIcon(R.drawable.ic_action_selectnone);
                mAdapter.notifyDataSetChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        IconRequest.saveInstanceState(outState);
    }
}