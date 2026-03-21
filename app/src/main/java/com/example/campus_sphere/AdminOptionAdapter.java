package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminOptionAdapter extends RecyclerView.Adapter<AdminOptionAdapter.VH> {

    public interface OnOptionClickListener {
        void onOptionClick(AdminOptionItem item);
    }

    private final List<AdminOptionItem> items;
    private final OnOptionClickListener listener;

    public AdminOptionAdapter(List<AdminOptionItem> items, OnOptionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_option, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AdminOptionItem item = items.get(position);
        holder.title.setText(item.title);
        holder.icon.setImageResource(item.iconRes);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOptionClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.optionIcon);
            title = itemView.findViewById(R.id.optionTitle);
        }
    }
}

