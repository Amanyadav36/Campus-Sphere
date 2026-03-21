package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminActionAdapter extends RecyclerView.Adapter<AdminActionAdapter.VH> {

    public interface OnActionClickListener {
        void onActionClick(AdminActionItem item);
    }

    private final List<AdminActionItem> items;
    private final OnActionClickListener listener;

    public AdminActionAdapter(List<AdminActionItem> items, OnActionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_action_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AdminActionItem item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        holder.icon.setImageResource(item.iconRes);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onActionClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.actionIcon);
            title = itemView.findViewById(R.id.actionTitle);
            subtitle = itemView.findViewById(R.id.actionSubtitle);
        }
    }
}

