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

public class LeaderEventAdapter extends RecyclerView.Adapter<LeaderEventAdapter.ViewHolder> {

    private List<Event> eventList;
    private OnEventActionListener actionListener;

    public interface OnEventActionListener {
        void onEdit(Event event);
        void onDelete(Event event);
    }

    public LeaderEventAdapter(List<Event> eventList, OnEventActionListener listener) {
        this.eventList = eventList;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Points to our new layout file
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leader_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.title.setText(event.getTitle());
        holder.desc.setText(event.getDescription());

        // Load Image
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.image);
        }

        // Button Click Listeners
        holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(event));
        holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(event));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, desc;
        Button btnEdit, btnDelete;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.eventTitle);
            desc = itemView.findViewById(R.id.eventDesc);
            image = itemView.findViewById(R.id.eventImage);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}