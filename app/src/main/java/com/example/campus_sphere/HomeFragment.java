package com.example.campus_sphere;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private com.google.android.material.button.MaterialButton btnMyEvents, btnAllEvents;
    private FloatingActionButton chatFab;
    private EditText searchBar;
    private EventAdapter adapter;
    private List<Event> currentList;
    private List<Event> myEventsList;
    private List<Event> otherEventsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_home, container, false);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        btnMyEvents = view.findViewById(R.id.btnMyEvents);
        btnAllEvents = view.findViewById(R.id.btnAllEvents);
        chatFab = view.findViewById(R.id.chatFab);
        searchBar = view.findViewById(R.id.search_bar);

        currentList = new ArrayList<>();
        myEventsList = new ArrayList<>();
        otherEventsList = new ArrayList<>();
        
        adapter = new EventAdapter(currentList, event -> {
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("event_data", event); // Pass the clicked event
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        btnAllEvents.setOnClickListener(v -> {
            btnAllEvents.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F3F0FF")));
            btnAllEvents.setTextColor(android.graphics.Color.parseColor("#6C5CE7"));
            btnMyEvents.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFFFFF")));
            btnMyEvents.setTextColor(android.graphics.Color.parseColor("#636E72"));
            
            currentList.clear();
            currentList.addAll(otherEventsList);
            adapter.notifyDataSetChanged();
        });

        btnMyEvents.setOnClickListener(v -> {
            btnMyEvents.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F3F0FF")));
            btnMyEvents.setTextColor(android.graphics.Color.parseColor("#6C5CE7"));
            btnAllEvents.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFFFFF")));
            btnAllEvents.setTextColor(android.graphics.Color.parseColor("#636E72"));
            
            currentList.clear();
            currentList.addAll(myEventsList);
            adapter.notifyDataSetChanged();
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Fetch Real Data
        fetchEventsFromFirestore();

        chatFab.setOnClickListener(v -> startActivity(new Intent(getActivity(), ChatBotActivity.class)));

        return view;
    }

    private void filterEvents(String query) {
        List<Event> filtered = new ArrayList<>();
        // Determine which list is currently active by checking button text color
        boolean isAllEventsActive = (btnAllEvents.getCurrentTextColor() == Color.parseColor("#6C5CE7"));
        List<Event> sourceList = isAllEventsActive ? otherEventsList : myEventsList;

        if (query.trim().isEmpty()) {
            filtered.addAll(sourceList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Event event : sourceList) {
                boolean matchesTitle = event.getTitle() != null && event.getTitle().toLowerCase().contains(lowerQuery);
                boolean matchesDesc = event.getDescription() != null && event.getDescription().toLowerCase().contains(lowerQuery);
                boolean matchesCategory = event.getCategory() != null && event.getCategory().toLowerCase().contains(lowerQuery);
                boolean matchesVenue = event.getVenue() != null && event.getVenue().toLowerCase().contains(lowerQuery);
                
                if (matchesTitle || matchesDesc || matchesCategory || matchesVenue) {
                    filtered.add(event);
                }
            }
        }
        currentList.clear();
        currentList.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void fetchEventsFromFirestore() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("club_members")
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(memberDocs -> {
                    java.util.Set<String> joinedClubIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : memberDocs.getDocuments()) {
                        String clubId = doc.getString("clubId");
                        if (clubId != null) joinedClubIds.add(clubId);
                    }

                    FirebaseFirestore.getInstance().collection("events")
                            .get()
                            .addOnSuccessListener(eventDocs -> {
                                myEventsList.clear();
                                otherEventsList.clear();

                                for (DocumentSnapshot doc : eventDocs.getDocuments()) {
                                    Event event = doc.toObject(Event.class);
                                    if (event != null) {
                                        if (joinedClubIds.contains(event.getCreatorId())) {
                                            myEventsList.add(event);
                                        } else {
                                            otherEventsList.add(event);
                                        }
                                    }
                                }

                                currentList.clear();
                                currentList.addAll(otherEventsList);
                                filterEvents(searchBar.getText().toString()); // Apply current search filter
                                
                                if (currentList.isEmpty() && getContext() != null) {
                                    Toast.makeText(getContext(), "No events found.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) Toast.makeText(getContext(), "Error getting memberships", Toast.LENGTH_SHORT).show();
                });
    }
}