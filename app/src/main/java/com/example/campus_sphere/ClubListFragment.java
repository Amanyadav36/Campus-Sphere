package com.example.campus_sphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClubListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClubAdapter adapter;
    private List<Club> clubList;
    private List<Club> myClubs;
    private List<Club> otherClubs;
    private TextView emptyStateText;
    private FirebaseFirestore db;
    private TabLayout tabLayout;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_club_list, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        recyclerView = view.findViewById(R.id.clubsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        tabLayout = view.findViewById(R.id.clubTabLayout);

        clubList = new ArrayList<>();
        myClubs = new ArrayList<>();
        otherClubs = new ArrayList<>();
        adapter = new ClubAdapter(clubList, club -> {
            if (getContext() != null) {
                ClubDetailsActivity.start(getContext(), club.getId());
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showClubs(myClubs);
                } else {
                    showClubs(otherClubs);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadClubs();

        return view;
    }

    private void loadClubs() {
        if (currentUserId == null) return;

        db.collection("club_members")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(memberSnapshot -> {
                    Set<String> joinedIds = new HashSet<>();
                    for (DocumentSnapshot doc : memberSnapshot.getDocuments()) {
                        String clubId = doc.getString("clubId");
                        if (clubId != null) joinedIds.add(clubId);
                    }

                    db.collection("users").whereEqualTo("role", "leader").get()
                            .addOnSuccessListener(clubSnapshot -> {
                                myClubs.clear();
                                otherClubs.clear();
                                if (!clubSnapshot.isEmpty()) {
                                    for (DocumentSnapshot doc : clubSnapshot.getDocuments()) {
                                        String clubId = doc.getId();
                                        String name = doc.getString("clubName");
                                        String handle = doc.getString("clubHandle");
                                        String bio = doc.getString("clubBio");
                                        String logo = doc.getString("clubLogo");
                                        String header = doc.getString("headerImage");
                                        Club club = new Club(clubId, name, handle, bio, logo, header);
                                        if (joinedIds.contains(clubId)) {
                                            club.setJoined(true);
                                            myClubs.add(club);
                                        } else {
                                            club.setJoined(false);
                                            otherClubs.add(club);
                                        }
                                    }
                                }

                                if (tabLayout.getSelectedTabPosition() == 0) {
                                    showClubs(myClubs);
                                } else {
                                    showClubs(otherClubs);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error loading clubs", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void showClubs(List<Club> source) {
        clubList.clear();
        clubList.addAll(source);
        adapter.notifyDataSetChanged();
        if (clubList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }
}
