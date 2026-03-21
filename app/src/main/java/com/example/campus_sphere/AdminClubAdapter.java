package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminClubAdapter extends RecyclerView.Adapter<AdminClubAdapter.VH> {

    public interface OnClubClickListener {
        void onClubClick(AdminClubRow club);
    }

    private final List<AdminClubRow> clubs;
    private final OnClubClickListener listener;

    public AdminClubAdapter(List<AdminClubRow> clubs, OnClubClickListener listener) {
        this.clubs = clubs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_club, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AdminClubRow c = clubs.get(position);
        holder.name.setText(c.name != null ? c.name : "Club");
        holder.handle.setText(c.handle != null && !c.handle.trim().isEmpty() ? "@" + c.handle : "@club");
        holder.leader.setText("Leader: " + (c.leaderEmail != null ? c.leaderEmail : "-"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClubClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return clubs != null ? clubs.size() : 0;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView handle;
        final TextView leader;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.adminClubName);
            handle = itemView.findViewById(R.id.adminClubHandle);
            leader = itemView.findViewById(R.id.adminClubLeader);
        }
    }
}

