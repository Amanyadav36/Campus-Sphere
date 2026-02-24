package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ListSectionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyState;
    private EventAdapter adapter;
    private final List<Event> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_section);
        
        String title = getIntent().getStringExtra("SECTION_TITLE");
        if (title == null) title = "Section List";
        
        TextView sectionTitle = findViewById(R.id.sectionTitle);
        sectionTitle.setText(title);
        
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);

        adapter = new EventAdapter(events, event -> {
            Intent intent = new Intent(ListSectionActivity.this, EventDetailsActivity.class);
            intent.putExtra("event_data", event);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if ("Bookmarks".equalsIgnoreCase(title)) {
            loadBookmarks();
        } else {
            showEmptyState(true);
        }
    }

    private void loadBookmarks() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            showEmptyState(true);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).collection("bookmarks").get()
                .addOnSuccessListener(snapshot -> {
                    events.clear();
                    if (snapshot.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    final int[] remaining = {snapshot.size()};
                    for (DocumentSnapshot bookmarkDoc : snapshot.getDocuments()) {
                        String eventId = bookmarkDoc.getString("eventId");
                        if (eventId == null || eventId.isEmpty()) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                finalizeList();
                            }
                            continue;
                        }

                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        String title = eventDoc.getString("title");
                                        String description = eventDoc.getString("description");
                                        String category = eventDoc.getString("category");
                                        String price = eventDoc.getString("price");
                                        String venue = eventDoc.getString("venue");
                                        String date = eventDoc.getString("date");
                                        String time = eventDoc.getString("time");
                                        String imageUrl = eventDoc.getString("imageUrl");
                                        String creatorId = eventDoc.getString("creatorId");
                                        Boolean attendanceEnabled = eventDoc.getBoolean("attendanceEnabled");
                                        Event event = new Event(
                                                eventId,
                                                title,
                                                description,
                                                category,
                                                price,
                                                venue,
                                                date,
                                                time,
                                                imageUrl,
                                                creatorId,
                                                attendanceEnabled != null && attendanceEnabled
                                        );
                                        events.add(event);
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        finalizeList();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        finalizeList();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> showEmptyState(true));
    }

    private void finalizeList() {
        adapter.notifyDataSetChanged();
        showEmptyState(events.isEmpty());
    }

    private void showEmptyState(boolean show) {
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
