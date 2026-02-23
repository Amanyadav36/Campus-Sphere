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

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnEventClickListener listener;

    // Interface to communicate with Activity
    public interface OnEventClickListener {
        void onRegisterClick(Event event);
    }

    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        
        holder.title.setText(event.getTitle());
        String cat = event.getCategory() != null ? event.getCategory() : "Campus Event";
        String ven = event.getVenue() != null ? event.getVenue() : "TBA";
        holder.desc.setText(cat + " • " + ven);

        if (event.getCreatorId() != null && !event.getCreatorId().isEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(event.getCreatorId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String clubName = doc.getString("clubName");
                            if (clubName == null || clubName.isEmpty()) clubName = doc.getString("name");
                            if (clubName != null && !clubName.isEmpty()) {
                                holder.desc.setText(clubName + " • " + ven);
                            }
                        }
                    });
        }

        String dt = (event.getDate() != null ? event.getDate() : "") + " " + (event.getTime() != null ? event.getTime() : "");
        if (holder.date != null) {
            holder.date.setText(dt.trim());
        }

        if (holder.price != null) {
            if (event.getAmountInPaise() > 0) {
                holder.price.setText("Starts at " + event.getPrice());
            } else {
                holder.price.setText("Free Entry");
            }
        }

        if (holder.btnBookmark != null) {
            holder.btnBookmark.setOnClickListener(v -> {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Event Bookmarked", android.widget.Toast.LENGTH_SHORT).show();
                holder.btnBookmark.setColorFilter(android.graphics.Color.WHITE);
                holder.btnBookmark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6C5CE7")));
            });
        }

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.eventImage);
        }

        holder.registerBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRegisterClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, desc, price, date;
        Button registerBtn;
        ImageView eventImage, btnBookmark;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.eventTitle);
            desc = itemView.findViewById(R.id.eventDesc);
            date = itemView.findViewById(R.id.eventDate);
            price = itemView.findViewById(R.id.eventPrice);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            
            // Details Btn mapped to Register Now
            registerBtn = itemView.findViewById(R.id.detailsBtn);
            eventImage = itemView.findViewById(R.id.eventImage);
        }
    }
}