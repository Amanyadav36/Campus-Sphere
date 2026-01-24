package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Make sure Glide is imported
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
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

        // Combine Date and Time
        String dateTime = event.getDate() + " • " + event.getTime();
        holder.desc.setText(dateTime + "\n📍 " + event.getVenue());

        // Show Category/Club Name
        holder.clubName.setText(event.getCategory());

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.eventImage);
        }

        holder.registerBtn.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Registered for " + event.getTitle(), Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, clubName, desc;
        Button registerBtn;
        ImageView eventImage; // Added Image View

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.eventTitle);
            clubName = itemView.findViewById(R.id.clubName);
            desc = itemView.findViewById(R.id.eventDesc);
            registerBtn = itemView.findViewById(R.id.registerBtn);
            eventImage = itemView.findViewById(R.id.eventImage); // Ensure this ID exists in event.xml
        }
    }
}