package com.example.campus_sphere;

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

public class AdminClubsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClubAdapter adapter;
    private final List<Club> clubs = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_admin_clubs, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.adminClubsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClubAdapter(clubs, club -> {
            if (getContext() != null) {
                ClubDetailsActivity.start(getContext(), club.getId());
            }
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.adminCreateClubBtn).setOnClickListener(v -> {
            if (getContext() != null) {
                startActivity(new android.content.Intent(getContext(), AdminCreateClubActivity.class));
            }
        });

        fetchClubs();
        return view;
    }

    private void fetchClubs() {
        db.collection("users").whereEqualTo("role", "leader").get()
                .addOnSuccessListener(snapshot -> {
                    clubs.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String clubId = doc.getId();
                        String name = doc.getString("clubName");
                        String handle = doc.getString("clubHandle");
                        String bio = doc.getString("clubBio");
                        String logo = doc.getString("clubLogo");
                        String header = doc.getString("headerImage");
                        clubs.add(new Club(clubId, name, handle, bio, logo, header));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load clubs", Toast.LENGTH_SHORT).show()
                );
    }

    // Club creation now handled in AdminCreateClubActivity
}
