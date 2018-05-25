package com.afollestad.iconrequestsample

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.iconrequest.AppModel
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.list_item_app.view.icon
import kotlinx.android.synthetic.main.list_item_app.view.title

typealias SelectionListener = ((Int, AppModel) -> (Unit))

/** @author Aidan Follestad (afollestad) */
internal class MainAdapter : RecyclerView.Adapter<MainAdapter.MainVH>() {

  private var appsList: MutableList<AppModel> = mutableListOf()
  private var listener: SelectionListener? = null

  fun setAppsList(appsList: List<AppModel>?) {
    this.appsList = appsList?.toMutableList() ?: mutableListOf()
    notifyDataSetChanged()
  }

  fun update(app: AppModel) {
    for (i in appsList.indices) {
      if (app.code == appsList[i].code) {
        appsList[i] = app
        notifyItemChanged(i)
        break
      }
    }
  }

  fun setListener(listener: SelectionListener) {
    this.listener = listener
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): MainVH {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.list_item_app, parent, false)
    return MainVH(view, this)
  }

  override fun getItemCount(): Int {
    return appsList.size
  }

  override fun onBindViewHolder(
    holder: MainVH,
    position: Int
  ) {
    val app = appsList[position]
    holder.bind(app)
  }

  internal class MainVH(
    itemView: View,
    private val adapter: MainAdapter
  ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    init {
      itemView.setOnClickListener(this)
    }

    fun bind(model: AppModel) {
      Glide.with(itemView.icon)
          .load(model)
          .into(itemView.icon)
      itemView.icon.alpha = 1f
      itemView.title.text = model.name
      itemView.title.alpha = 1f
      itemView.isActivated = model.selected
    }

    override fun onClick(view: View) {
      val appModel = adapter.appsList[adapterPosition]
      adapter.listener?.invoke(adapterPosition, appModel)
    }
  }
}
