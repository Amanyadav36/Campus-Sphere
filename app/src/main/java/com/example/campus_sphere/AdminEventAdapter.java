package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.EventViewHolder> {

    public interface OnEventActionListener {
        void onView(Event event);
        void onToggleFeature(Event event, boolean feature);
        void onDelete(Event event);
    }

    private final List<AdminEventItem> events;
    private final OnEventActionListener listener;

    public AdminEventAdapter(List<AdminEventItem> events, OnEventActionListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        AdminEventItem item = events.get(position);
        Event event = item.getEvent();

        holder.title.setText(event.getTitle() != null ? event.getTitle() : "Untitled");
        String meta = (event.getDate() != null ? event.getDate() : "No date")
                + " • " + (event.getVenue() != null ? event.getVenue() : "No venue");
        holder.meta.setText(meta);
        holder.price.setText(event.getPrice() != null ? event.getPrice() : "Free");

        holder.featureBtn.setText(item.isFeatured() ? "Unfeature" : "Feature");

        holder.viewBtn.setOnClickListener(v -> listener.onView(event));
        holder.featureBtn.setOnClickListener(v -> listener.onToggleFeature(event, !item.isFeatured()));
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView meta;
        TextView price;
        Button viewBtn;
        Button featureBtn;
        Button deleteBtn;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.adminEventTitle);
            meta = itemView.findViewById(R.id.adminEventMeta);
            price = itemView.findViewById(R.id.adminEventPrice);
            viewBtn = itemView.findViewById(R.id.adminEventViewBtn);
            featureBtn = itemView.findViewById(R.id.adminEventFeatureBtn);
            deleteBtn = itemView.findViewById(R.id.adminEventDeleteBtn);
        }
    }
}
