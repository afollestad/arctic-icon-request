package com.afollestad.iconrequestsample

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.afollestad.assent.Assent
import com.afollestad.assent.AssentActivity
import com.afollestad.assent.PermissionResultSet
import com.afollestad.iconrequest.PolarConfig
import com.afollestad.iconrequest.PolarRequest
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_main.fab
import kotlinx.android.synthetic.main.activity_main.list
import kotlinx.android.synthetic.main.activity_main.progress
import kotlinx.android.synthetic.main.activity_main.rootView
import kotlinx.android.synthetic.main.activity_main.toolbar
import java.io.File

class MainActivity : AssentActivity(), Toolbar.OnMenuItemClickListener {

  private lateinit var adapter: MainAdapter

  private var request: PolarRequest? = null
  private var dialog: MaterialDialog? = null

  fun onClickFab() {
    if (!Assent.isPermissionGranted(Assent.WRITE_EXTERNAL_STORAGE)) {
      Assent.requestPermissions(
          { results: PermissionResultSet ->
            if (results.allPermissionsGranted()) {
              request!!.send()
                  .subscribe()
            } else {
              Snackbar.make(rootView!!, R.string.permission_denied, Snackbar.LENGTH_LONG)
                  .show()
            }
          },
          69,
          Assent.WRITE_EXTERNAL_STORAGE
      )
      return
    }
    request!!.send()
        .subscribe()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    toolbar.inflateMenu(R.menu.menu_main)
    toolbar.setOnMenuItemClickListener(this)
    fab.hide()

    adapter = MainAdapter()
    adapter.setListener { _, app -> request?.toggleSelection(app) }

    val lm = GridLayoutManager(this, resources.getInteger(R.integer.grid_width))
    list.layoutManager = lm
    list.adapter = adapter

    val config = PolarConfig.create(this)
        .emailRecipient("fake-email@helloworld.com")
        .build()
    request = PolarRequest.make(this, savedInstanceState)
        .config(config)
        .uriTransformer { uri ->
          FileProvider.getUriForFile(
              this@MainActivity,
              BuildConfig.APPLICATION_ID + ".fileProvider",
              File(uri.path)
          )
        }

    request!!
        .loading()
        .subscribe {
          progress.visibility = if (it) VISIBLE else GONE
        }
        .unsubscribeOnDetach(rootView)

    request!!
        .loaded()
        .subscribe { loadResult ->
          if (!loadResult.success()) {
            adapter.setAppsList(null)
            Snackbar.make(rootView!!, loadResult.error()!!.message!!, Snackbar.LENGTH_LONG)
                .show()
            request!!.loaded()
                .subscribe()
          } else {
            adapter.setAppsList(loadResult.apps())
            invalidateToolbar()
          }
        }
        .unsubscribeOnDetach(rootView)

    request!!
        .selectionChange()
        .subscribe {
          adapter.update(it)
          invalidateToolbar()
        }
        .unsubscribeOnDetach(rootView)

    request!!
        .sending()
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

    request!!
        .sent()
        .subscribe {
          if (it.success()) {
            Snackbar.make(rootView!!, R.string.request_sent, Snackbar.LENGTH_SHORT)
                .show()
          } else {
            Snackbar.make(rootView!!, it.error()!!.message!!, Snackbar.LENGTH_LONG)
                .show()
          }
        }
        .unsubscribeOnDetach(rootView)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      var flags = window.decorView.systemUiVisibility
      flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
      window.decorView.systemUiVisibility = flags
    }
  }

  override fun onResume() {
    super.onResume()
    if (request!!.loadedApps.isEmpty()) {
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
