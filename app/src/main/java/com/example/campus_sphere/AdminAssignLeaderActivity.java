package com.example.campus_sphere;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminAssignLeaderActivity extends AppCompatActivity {

    private Spinner spinnerClub;
    private EditText inputEmail;
    private Button btnAssign;
    private TextView tvStatus;

    private final List<IdName> clubs = new ArrayList<>();
    private ArrayAdapter<String> clubAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_assign_leader);

        db = FirebaseFirestore.getInstance();

        spinnerClub = findViewById(R.id.spinnerClubToAssign);
        inputEmail = findViewById(R.id.inputNewLeaderEmail);
        btnAssign = findViewById(R.id.btnAssignLeader);
        tvStatus = findViewById(R.id.tvAssignLeaderStatus);

        clubAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerClub.setAdapter(clubAdapter);

        btnAssign.setOnClickListener(v -> assignLeader());

        loadClubs();
    }

    private void setStatus(String text) {
        if (tvStatus != null) tvStatus.setText(text != null ? text : "");
    }

    private void loadClubs() {
        setStatus("Loading clubs...");
        clubs.clear();
        clubAdapter.clear();

        db.collection("clubs").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String clubId = doc.getId();
                        String name = doc.getString("name");
                        if (name == null) name = doc.getString("clubName");
                        if (name == null) name = clubId;
                        clubs.add(new IdName(clubId, name));
                    }

                    if (clubs.isEmpty()) {
                        loadClubsFallback();
                        return;
                    }

                    for (IdName c : clubs) clubAdapter.add(c.name);
                    clubAdapter.notifyDataSetChanged();
                    setStatus("");
                })
                .addOnFailureListener(e -> loadClubsFallback());
    }

    private void loadClubsFallback() {
        db.collection("users").whereEqualTo("role", "leader").get()
                .addOnSuccessListener(snap -> {
                    clubs.clear();
                    clubAdapter.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String clubId = doc.getId(); // legacy: clubId == leaderId
                        String name = doc.getString("clubName");
                        if (name == null) name = clubId;
                        clubs.add(new IdName(clubId, name));
                    }
                    for (IdName c : clubs) clubAdapter.add(c.name);
                    clubAdapter.notifyDataSetChanged();
                    setStatus("");
                })
                .addOnFailureListener(e -> setStatus("Failed to load clubs: " + e.getMessage()));
    }

    private void assignLeader() {
        int pos = spinnerClub.getSelectedItemPosition();
        if (pos < 0 || pos >= clubs.size()) {
            Toast.makeText(this, "Select a club", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldLeaderId = clubs.get(pos).id; // legacy: clubId == old leader uid
        String email = inputEmail.getText().toString().trim();
        if (email.isEmpty()) {
            inputEmail.setError("Required");
            return;
        }

        btnAssign.setEnabled(false);
        setStatus("Looking up user...");

        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.isEmpty()) {
                        btnAssign.setEnabled(true);
                        setStatus("No user found with that email.");
                        return;
                    }

                    DocumentSnapshot newLeaderDoc = userSnap.getDocuments().get(0);
                    String newLeaderId = newLeaderDoc.getId();

                    String existingRole = newLeaderDoc.getString("role");
                    if ("leader".equals(existingRole) && newLeaderDoc.getString("clubName") != null) {
                        btnAssign.setEnabled(true);
                        setStatus("User already leads a club.");
                        return;
                    }

                    transferClubOwnership(oldLeaderId, newLeaderId, email);
                })
                .addOnFailureListener(e -> {
                    btnAssign.setEnabled(true);
                    setStatus("Lookup failed: " + e.getMessage());
                });
    }

    private void transferClubOwnership(String oldLeaderId, String newLeaderId, String newLeaderEmail) {
        setStatus("Transferring club...");

        DocumentReference oldLeaderRef = db.collection("users").document(oldLeaderId);
        DocumentReference newLeaderRef = db.collection("users").document(newLeaderId);
        DocumentReference oldClubRef = db.collection("clubs").document(oldLeaderId);
        DocumentReference newClubRef = db.collection("clubs").document(newLeaderId);

        oldLeaderRef.get().addOnSuccessListener(oldLeaderDoc -> {
            if (!oldLeaderDoc.exists()) {
                btnAssign.setEnabled(true);
                setStatus("Club leader user not found.");
                return;
            }

            Map<String, Object> clubFields = new HashMap<>();
            clubFields.put("role", "leader");
            clubFields.put("clubName", oldLeaderDoc.getString("clubName"));
            clubFields.put("clubHandle", oldLeaderDoc.getString("clubHandle"));
            clubFields.put("clubBio", oldLeaderDoc.getString("clubBio"));
            clubFields.put("clubLogo", oldLeaderDoc.getString("clubLogo"));
            clubFields.put("headerImage", oldLeaderDoc.getString("headerImage"));
            clubFields.put("clubId", newLeaderId);

            // Pull club doc data if present, otherwise create one from user fields.
            oldClubRef.get().addOnSuccessListener(oldClubDoc -> {
                Map<String, Object> clubDocData = new HashMap<>();
                if (oldClubDoc.exists() && oldClubDoc.getData() != null) {
                    clubDocData.putAll(oldClubDoc.getData());
                } else {
                    clubDocData.put("name", oldLeaderDoc.getString("clubName"));
                    clubDocData.put("handle", oldLeaderDoc.getString("clubHandle"));
                    clubDocData.put("bio", oldLeaderDoc.getString("clubBio"));
                    clubDocData.put("logoUrl", oldLeaderDoc.getString("clubLogo"));
                    clubDocData.put("headerUrl", oldLeaderDoc.getString("headerImage"));
                }
                clubDocData.put("leaderId", newLeaderId);
                clubDocData.put("leaderEmail", newLeaderEmail);

                // 1) Update new leader user with club fields (for legacy screens that read clubs from users doc)
                // 2) Demote old leader user
                WriteBatch batch = db.batch();

                batch.update(newLeaderRef, clubFields);

                Map<String, Object> demote = new HashMap<>();
                demote.put("role", "user");
                demote.put("clubId", null);
                demote.put("clubName", null);
                demote.put("clubHandle", null);
                demote.put("clubBio", null);
                demote.put("clubLogo", null);
                demote.put("headerImage", null);
                batch.update(oldLeaderRef, demote);

                // Clubs collection: create new club doc, delete old doc
                batch.set(newClubRef, clubDocData);
                batch.delete(oldClubRef);

                batch.commit()
                        .addOnSuccessListener(aVoid -> updateEventsAndMembers(oldLeaderId, newLeaderId))
                        .addOnFailureListener(e -> {
                            btnAssign.setEnabled(true);
                            setStatus("Transfer failed: " + e.getMessage());
                        });
            }).addOnFailureListener(e -> {
                btnAssign.setEnabled(true);
                setStatus("Transfer failed: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            btnAssign.setEnabled(true);
            setStatus("Transfer failed: " + e.getMessage());
        });
    }

    private void updateEventsAndMembers(String oldLeaderId, String newLeaderId) {
        setStatus("Updating events and members...");

        db.collection("events").whereEqualTo("creatorId", oldLeaderId).get()
                .addOnSuccessListener(eventsSnap -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : eventsSnap.getDocuments()) {
                        batch.update(doc.getReference(), "creatorId", newLeaderId);
                        batch.update(doc.getReference(), "clubId", newLeaderId);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> updateClubMembers(oldLeaderId, newLeaderId))
                            .addOnFailureListener(e -> updateClubMembers(oldLeaderId, newLeaderId));
                })
                .addOnFailureListener(e -> updateClubMembers(oldLeaderId, newLeaderId));
    }

    private void updateClubMembers(String oldLeaderId, String newLeaderId) {
        db.collection("club_members").whereEqualTo("clubId", oldLeaderId).get()
                .addOnSuccessListener(membersSnap -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : membersSnap.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null) continue;
                        Map<String, Object> data = doc.getData() != null ? new HashMap<>(doc.getData()) : new HashMap<>();
                        data.put("clubId", newLeaderId);
                        batch.set(db.collection("club_members").document(newLeaderId + "_" + userId), data);
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                AdminAuditLogger.log("CLUB_ASSIGN_LEADER", "club", oldLeaderId, oldLeaderId, newLeaderId);
                                btnAssign.setEnabled(true);
                                setStatus("Leader assigned.");
                                Toast.makeText(this, "Leader assigned", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnAssign.setEnabled(true);
                                setStatus("Leader assigned, but member migration failed.");
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    btnAssign.setEnabled(true);
                    setStatus("Leader assigned, but member migration failed.");
                    finish();
                });
    }

    private static final class IdName {
        final String id;
        final String name;

        IdName(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

