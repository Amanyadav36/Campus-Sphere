package com.example.campus_sphere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LeaderHomeFragment extends Fragment {

    private TextView eventsCount, membersCount;
    private RecyclerView recyclerView;
    private LeaderEventAdapter adapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private String currentUid;
    private ActivityResultLauncher<Intent> editLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leader_home, container, false);

        // 1. Initialize
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        eventsCount = view.findViewById(R.id.eventsCount);
        membersCount = view.findViewById(R.id.membersCount);
        recyclerView = view.findViewById(R.id.leaderEventsRecycler);

        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        fetchMyEvents();
                    }
                });

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventList = new ArrayList<>();

        // 3. Connect Adapter
        adapter = new LeaderEventAdapter(eventList, new LeaderEventAdapter.OnEventActionListener() {
            @Override
            public void onEdit(Event event) {
                Intent intent = new Intent(getContext(), EditEventActivity.class);
                intent.putExtra("event_data", event);
                editLauncher.launch(intent);
            }

            @Override
            public void onDelete(Event event) {
                confirmDelete(event);
            }
        });
        recyclerView.setAdapter(adapter);

        // 4. Fetch Data
        fetchMyEvents();

        return view;
    }

    private void fetchMyEvents() {
        // Query: Get events where "creatorId" equals the current User ID
        db.collection("events")
                .whereEqualTo("creatorId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Event event = doc.toObject(Event.class);
                            eventList.add(event);
                        }
                        adapter.notifyDataSetChanged();

                        // ✅ UPDATE ACTIVE EVENTS COUNT
                        eventsCount.setText(String.valueOf(eventList.size()));

                    } else {
                        eventsCount.setText("0");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show()
                );

        // Optional: Static placeholder for members until you implement member joining
        membersCount.setText("0");
    }

    private void confirmDelete(Event event) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Event?")
                .setMessage("Are you sure you want to delete '" + event.getTitle() + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Firebase
                    db.collection("events").document(event.getEventId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Event Deleted", Toast.LENGTH_SHORT).show();
                                fetchMyEvents(); // Refresh list and counts
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Deletion failed", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
