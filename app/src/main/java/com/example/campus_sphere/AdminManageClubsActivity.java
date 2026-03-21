package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminManageClubsActivity extends AppCompatActivity {

    private final List<AdminClubRow> clubs = new ArrayList<>();
    private AdminClubAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_clubs);

        db = FirebaseFirestore.getInstance();

        RecyclerView rv = findViewById(R.id.adminClubsRecycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminClubAdapter(clubs, club -> {
            if (club == null || club.clubId == null) return;
            String[] options = {"View Club", "Edit Club"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(club.name != null ? club.name : "Club")
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            ClubDetailsActivity.start(this, club.clubId);
                        } else {
                            Intent intent = new Intent(this, AdminEditClubActivity.class);
                            intent.putExtra(AdminEditClubActivity.EXTRA_CLUB_ID, club.clubId);
                            startActivity(intent);
                        }
                    })
                    .show();
        });
        rv.setAdapter(adapter);

        Button createBtn = findViewById(R.id.adminCreateClubBtn);
        createBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminCreateClubActivity.class)));

        fetchClubs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchClubs();
    }

    private void fetchClubs() {
        db.collection("clubs")
                .get()
                .addOnSuccessListener(snap -> {
                    clubs.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String clubId = doc.getId();
                        String name = doc.getString("name");
                        if (name == null) name = doc.getString("clubName");
                        String handle = doc.getString("handle");
                        if (handle == null) handle = doc.getString("clubHandle");
                        String leaderEmail = doc.getString("leaderEmail");
                        clubs.add(new AdminClubRow(clubId, name, handle, leaderEmail));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load clubs", Toast.LENGTH_SHORT).show();
                    // Fallback to old storage (leaders in users collection)
                    fetchClubsFromUsersFallback();
                });
    }

    private void fetchClubsFromUsersFallback() {
        db.collection("users").whereEqualTo("role", "leader").get()
                .addOnSuccessListener(snapshot -> {
                    clubs.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String clubId = doc.getId();
                        String name = doc.getString("clubName");
                        String handle = doc.getString("clubHandle");
                        String leaderEmail = doc.getString("email");
                        clubs.add(new AdminClubRow(clubId, name, handle, leaderEmail));
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
