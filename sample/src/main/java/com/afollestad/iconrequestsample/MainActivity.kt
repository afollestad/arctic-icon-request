package com.afollestad.iconrequestsample

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.afollestad.iconrequest.ArcticConfig
import com.afollestad.iconrequest.ArcticRequest
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_main.fab
import kotlinx.android.synthetic.main.activity_main.list
import kotlinx.android.synthetic.main.activity_main.progress
import kotlinx.android.synthetic.main.activity_main.rootView
import kotlinx.android.synthetic.main.activity_main.toolbar
import java.io.File

class MainActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

  companion object {
    private const val WRITE_EXTERNAL_STORAGE_RQ = 69;
  }

  private lateinit var adapter: MainAdapter

  private var request: ArcticRequest? = null
  private var dialog: MaterialDialog? = null

  private fun onClickFab() {
    val permissionGrantedOrNot = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
    if (permissionGrantedOrNot == PackageManager.PERMISSION_GRANTED) {
      request!!.send()
          .subscribe()
    } else {
      ActivityCompat.requestPermissions(
          this, arrayOf(WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_RQ
      );
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == WRITE_EXTERNAL_STORAGE_RQ && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      onClickFab()
    } else {
      Snackbar.make(rootView!!, R.string.permission_denied, Snackbar.LENGTH_LONG)
          .show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    toolbar.inflateMenu(R.menu.menu_main)
    toolbar.setOnMenuItemClickListener(this)

    fab.hide()
    fab.setOnClickListener { onClickFab() }

    adapter = MainAdapter()
    adapter.setListener { _, app -> request?.toggleSelection(app) }

    val lm = GridLayoutManager(this, resources.getInteger(R.integer.grid_width))
    list.layoutManager = lm
    list.adapter = adapter

    val config = ArcticConfig(emailRecipient = "fake-email@helloworld.com")

    request = ArcticRequest.make(this, savedInstanceState)
        .setConfig(config)
        .setUriTransformer {
          FileProvider.getUriForFile(
              this@MainActivity,
              BuildConfig.APPLICATION_ID + ".fileProvider",
              File(it.path)
          )
        }

    with(request!!) {
      loading()
          .subscribe {
            progress.visibility = if (it) VISIBLE else GONE
          }
          .unsubscribeOnDetach(rootView)

      loaded()
          .subscribe { loadResult ->
            if (!loadResult.success) {
              adapter.setAppsList(null)
              Snackbar.make(rootView!!, loadResult.error!!.message!!, Snackbar.LENGTH_LONG)
                  .show()
              request!!.loaded()
                  .subscribe()
            } else {
              adapter.setAppsList(loadResult.apps)
              invalidateToolbar()
            }
          }
          .unsubscribeOnDetach(rootView)

      selectionChange()
          .subscribe {
            adapter.update(it)
            invalidateToolbar()
          }
          .unsubscribeOnDetach(rootView)

      sending()
          .subscribe {
            if (it) {
              dialog = MaterialDialog.Builder(this@MainActivity)
                  .content(R.string.preparing_your_request)
                  .progress(true, -1)
                  .cancelable(false)
                  .canceledOnTouchOutside(false)
                  .show()
            } else {
              dialog?.dismiss()
            }
          }
          .unsubscribeOnDetach(rootView)

      sent()
          .subscribe {
            if (it.success) {
              Snackbar.make(rootView!!, R.string.request_sent, Snackbar.LENGTH_SHORT)
                  .show()
            } else {
              Snackbar.make(rootView!!, it.error!!.message!!, Snackbar.LENGTH_LONG)
                  .show()
            }
          }
          .unsubscribeOnDetach(rootView)
    }

    if (SDK_INT >= VERSION_CODES.O) {
      var flags = window.decorView.systemUiVisibility
      flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
      window.decorView.systemUiVisibility = flags
    }
  }

  override fun onResume() {
    super.onResume()
    if (request!!.getLoadedApps().isEmpty()) {
      request!!.load()
          .subscribe()
    }
  }

  private fun invalidateToolbar() {
    request!!
        .selectedApps
        .subscribe { appModels ->
          val selectedCount = appModels.size
          if (selectedCount == 0) {
            fab.hide()
            toolbar.setTitle(R.string.app_name)
            toolbar
                .menu
                .findItem(R.id.selectAllNone)
                .setIcon(R.drawable.ic_action_selectall)
          } else {
            fab.show()
            toolbar.title = getString(R.string.app_name_x, selectedCount)
            toolbar
                .menu
                .findItem(R.id.selectAllNone)
                .setIcon(R.drawable.ic_action_selectall)
          }
        }
        .unsubscribeOnDetach(rootView)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    request!!.saveInstance(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()
    request = null
  }

  override fun onPause() {
    super.onPause()
    dialog?.dismiss()
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    if (item.itemId == R.id.selectAllNone) {
      if (!request!!.selectedApps.blockingGet().isEmpty()) {
        request!!.deselectAll()
        item.setIcon(R.drawable.ic_action_selectall)
      } else {
        request!!.selectAll()
        item.setIcon(R.drawable.ic_action_selectnone)
      }
      return true
    }
    return false
  }
}
