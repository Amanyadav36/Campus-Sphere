package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ClubMemberAdminAdapter extends RecyclerView.Adapter<ClubMemberAdminAdapter.MemberViewHolder> {

    public interface OnPromoteListener {
        void onPromote(User user);
    }

    private final List<User> members;
    private final String leaderId;
    private final OnPromoteListener listener;

    public ClubMemberAdminAdapter(List<User> members, String leaderId, OnPromoteListener listener) {
        this.members = members;
        this.leaderId = leaderId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = members.get(position);
        holder.name.setText(user.getName() != null ? user.getName() : "Member");

        String details = (user.getBranch() != null ? user.getBranch() : "N/A")
                + " • " + (user.getEnrollment() != null ? user.getEnrollment() : "N/A");
        holder.details.setText(details);

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImage())
                    .circleCrop()
                    .into(holder.image);
        }

        boolean isLeader = user.getUid() != null && user.getUid().equals(leaderId);
        holder.promoteBtn.setEnabled(!isLeader);
        holder.promoteBtn.setText(isLeader ? "Leader" : "Promote");
        holder.promoteBtn.setOnClickListener(v -> {
            if (!isLeader && listener != null) {
                listener.onPromote(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        TextView details;
        Button promoteBtn;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.memberImage);
            name = itemView.findViewById(R.id.memberName);
            details = itemView.findViewById(R.id.memberDetails);
            promoteBtn = itemView.findViewById(R.id.promoteBtn);
        }
    }
}
