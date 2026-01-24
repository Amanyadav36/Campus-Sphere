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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnMyEvents, btnAllEvents;
    private FloatingActionButton chatFab;
    private EventAdapter adapter;
    private List<Event> allEventsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_home, container, false);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        btnMyEvents = view.findViewById(R.id.btnMyEvents);
        btnAllEvents = view.findViewById(R.id.btnAllEvents);
        chatFab = view.findViewById(R.id.chatFab);

        allEventsList = new ArrayList<>();
        adapter = new EventAdapter(allEventsList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Fetch Real Data
        fetchEventsFromFirestore();

        chatFab.setOnClickListener(v -> startActivity(new Intent(getActivity(), ChatBotActivity.class)));

        return view;
    }

    private void fetchEventsFromFirestore() {
        FirebaseFirestore.getInstance().collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEventsList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Event event = doc.toObject(Event.class);
                            allEventsList.add(event);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "No events found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show()
                );
    }
}