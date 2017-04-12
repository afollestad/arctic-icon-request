package com.afollestad.iconrequestsample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.iconrequest.AppModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Aidan Follestad (afollestad)
 */
class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainVH> {

  interface SelectionListener {

    void onSelection(int index, AppModel app);
  }

  private List<AppModel> appsList;
  private SelectionListener listener;

  MainAdapter() {
  }

  void setAppsList(List<AppModel> appsList) {
    this.appsList = appsList;
    notifyDataSetChanged();
  }

  void update(AppModel app) {
    for (int i = 0; i < appsList.size(); i++) {
      if (app.equals(appsList.get(i))) {
        appsList.set(i, app);
        notifyItemChanged(i);
        break;
      }
    }
  }

  void setListener(SelectionListener listener) {
    this.listener = listener;
  }

  @Override
  public MainVH onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_item_app, parent, false);
    return new MainVH(view, this);
  }

  @Override
  public int getItemCount() {
    return appsList != null ? appsList.size() : 0;
  }

  @Override
  public void onBindViewHolder(MainVH holder, int position) {
    final AppModel app = appsList.get(position);
    app.loadIcon(holder.icon);

    holder.title.setText(app.name());
    holder.title.setAlpha(1f);
    holder.icon.setAlpha(1f);

    holder.itemView.setActivated(app.selected());
  }

  static class MainVH extends RecyclerView.ViewHolder implements View.OnClickListener {

    @BindView(R.id.icon)
    ImageView icon;
    @BindView(R.id.title)
    TextView title;
    final MainAdapter adapter;

    MainVH(View itemView, MainAdapter adapter) {
      super(itemView);
      ButterKnife.bind(this, itemView);
      this.adapter = adapter;
      itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
      AppModel appModel = adapter.appsList.get(getAdapterPosition());
      if (adapter.listener != null) {
        adapter.listener.onSelection(getAdapterPosition(), appModel);
      }
    }
  }
}