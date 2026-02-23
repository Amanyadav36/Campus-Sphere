package com.example.campus_sphere;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminEventAdapter adapter;
    private final List<AdminEventItem> events = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_admin_events, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.adminEventsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminEventAdapter(events, new AdminEventAdapter.OnEventActionListener() {
            @Override
            public void onView(Event event) {
                Intent intent = new Intent(getContext(), EventDetailsActivity.class);
                intent.putExtra("event_data", event);
                startActivity(intent);
            }

            @Override
            public void onToggleFeature(Event event, boolean feature) {
                db.collection("events").document(event.getEventId())
                        .update("featured", feature)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(),
                                    feature ? "Event featured" : "Event unfeatured",
                                    Toast.LENGTH_SHORT).show();
                            AdminAuditLogger.log("EVENT_FEATURE", "event", event.getEventId(),
                                    feature ? "false" : "true",
                                    feature ? "true" : "false");
                            fetchEvents();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onDelete(Event event) {
                confirmDelete(event);
            }
        });
        recyclerView.setAdapter(adapter);

        fetchEvents();
        return view;
    }

    private void fetchEvents() {
        db.collection("events").get()
                .addOnSuccessListener(snapshot -> {
                    events.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) continue;
                        boolean featured = Boolean.TRUE.equals(doc.getBoolean("featured"));
                        events.add(new AdminEventItem(event, featured));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show()
                );
    }

    private void confirmDelete(Event event) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Event?")
                .setMessage("Delete '" + event.getTitle() + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(event.getEventId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                                AdminAuditLogger.log("EVENT_DELETE", "event", event.getEventId(),
                                        event.getTitle(), "deleted");
                                fetchEvents();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
