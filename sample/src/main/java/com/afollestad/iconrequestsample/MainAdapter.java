package com.afollestad.iconrequestsample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.iconrequest.App;
import com.afollestad.iconrequest.IconRequest;

import java.util.ArrayList;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainVH> {

    public MainAdapter() {
    }

    @Nullable
    public ArrayList<App> getApps() {
        if (IconRequest.get() != null)
            return IconRequest.get().getApps();
        return null;
    }

    @Override
    public MainVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_app, parent, false);
        return new MainVH(view, this);
    }

    @Override
    public int getItemCount() {
        return getApps() != null ? getApps().size() : 0;
    }

    @Override
    public void onBindViewHolder(MainVH holder, int position) {
        //noinspection ConstantConditions
        final App app = getApps().get(position);
        final Context c = holder.itemView.getContext();
        holder.title.setText(app.getName(c));
        holder.icon.setImageDrawable(app.getIcon(c));

        final IconRequest ir = IconRequest.get();
        holder.itemView.setActivated(ir != null && ir.isAppSelected(app));
    }

    public static class MainVH extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView icon;
        final TextView title;
        final MainAdapter adapter;

        public MainVH(View itemView, MainAdapter adapter) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            this.adapter = adapter;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final IconRequest ir = IconRequest.get();
            if (ir != null) {
                final App app = ir.getApps().get(getAdapterPosition());
                ir.toggleAppSelected(app);
                adapter.notifyItemChanged(getAdapterPosition());
            }
        }
    }
}