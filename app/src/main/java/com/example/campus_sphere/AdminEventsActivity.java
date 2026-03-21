package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminEventAdapter adapter;
    private final List<AdminEventItem> events = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_events);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.adminEventsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminEventAdapter(events, new AdminEventAdapter.OnEventActionListener() {
            @Override
            public void onView(Event event) {
                Intent intent = new Intent(AdminEventsActivity.this, EventDetailsActivity.class);
                intent.putExtra("event_data", event);
                startActivity(intent);
            }

            @Override
            public void onEdit(Event event) {
                Intent intent = new Intent(AdminEventsActivity.this, EditEventActivity.class);
                intent.putExtra("event_data", event);
                startActivity(intent);
            }

            @Override
            public void onToggleFeature(Event event, boolean feature) {
                db.collection("events").document(event.getEventId())
                        .update("featured", feature)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminEventsActivity.this,
                                    feature ? "Event featured" : "Event unfeatured",
                                    Toast.LENGTH_SHORT).show();
                            fetchEvents();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(AdminEventsActivity.this, "Update failed", Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onDelete(Event event) {
                confirmDelete(event);
            }
        });
        recyclerView.setAdapter(adapter);

        fetchEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchEvents();
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
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show()
                );
    }

    private void confirmDelete(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Event?")
                .setMessage("Delete '" + event.getTitle() + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    VenueSlotManager.deleteEventAndReleaseLock(db, event.getEventId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                fetchEvents();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
