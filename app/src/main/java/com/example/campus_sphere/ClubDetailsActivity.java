package com.example.campus_sphere;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ClubDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_CLUB_ID = "club_id";

    private TextView clubNameTitle, clubHandle, clubBio, joinedCount, emptyStateText, coverHint;
    private ImageView headerImage;
    private CircleImageView clubIcon;
    private Button joinClubBtn;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;

    private FirebaseFirestore db;
    private String clubId;
    private String currentUserId;
    private boolean isMember = false;
    private boolean isAdmin = false;

    private EventAdapter eventAdapter;
    private StudentAdapter memberAdapter;
    private List<Event> eventList;
    private List<User> memberList;

    public static void start(Context context, String clubId) {
        Intent intent = new Intent(context, ClubDetailsActivity.class);
        intent.putExtra(EXTRA_CLUB_ID, clubId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_manage_club);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        clubId = getIntent().getStringExtra(EXTRA_CLUB_ID);
        if (clubId == null) {
            Toast.makeText(this, "Club not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        clubNameTitle = findViewById(R.id.clubNameTitle);
        clubHandle = findViewById(R.id.clubHandle);
        clubBio = findViewById(R.id.clubBio);
        joinedCount = findViewById(R.id.joinedCount);
        headerImage = findViewById(R.id.headerImage);
        clubIcon = findViewById(R.id.clubIcon);
        joinClubBtn = findViewById(R.id.joinClubBtn);
        coverHint = findViewById(R.id.coverHint);
        recyclerView = findViewById(R.id.clubContentRecycler);
        tabLayout = findViewById(R.id.clubTabLayout);
        emptyStateText = findViewById(R.id.emptyStateText);

        findViewById(R.id.editProfileBtn).setVisibility(View.GONE);
        coverHint.setVisibility(View.GONE);

        headerImage.setOnClickListener(null);
        clubIcon.setOnClickListener(null);

        joinClubBtn.setVisibility(View.VISIBLE);
        joinClubBtn.setOnClickListener(v -> toggleMembership());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) {
                    loadMembers();
                } else if (tab.getPosition() == 1) {
                    recyclerView.setLayoutManager(new GridLayoutManager(ClubDetailsActivity.this, 2));
                    loadClubEvents();
                } else {
                    recyclerView.setLayoutManager(new LinearLayoutManager(ClubDetailsActivity.this));
                    loadClubEvents();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadClubData();
        loadClubEvents();
        loadAdminState();
        refreshMembershipState();
    }

    private void loadClubData() {
        db.collection("users").document(clubId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    String name = document.getString("clubName");
                    String handle = document.getString("clubHandle");
                    String bio = document.getString("clubBio");
                    String headerUrl = document.getString("headerImage");
                    String logoUrl = document.getString("clubLogo");

                    if (name != null) clubNameTitle.setText(name);
                    if (handle != null) clubHandle.setText("@" + handle);
                    if (bio != null) clubBio.setText(bio);

                    if (headerUrl != null && !headerUrl.isEmpty()) {
                        Glide.with(this).load(headerUrl).centerCrop().into(headerImage);
                    }
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(this).load(logoUrl).into(clubIcon);
                    }
                });
    }

    private void refreshMembershipState() {
        if (currentUserId == null) return;
        if (isAdmin) {
            joinClubBtn.setVisibility(View.GONE);
            return;
        }
        db.collection("club_members")
                .document(clubId + "_" + currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    isMember = doc.exists();
                    joinClubBtn.setText(isMember ? "Leave" : "Join");
                });
    }

    private void loadAdminState() {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    isAdmin = "admin".equals(role);
                    if (isAdmin) {
                        joinClubBtn.setVisibility(View.GONE);
                    }
                });
    }

    private void toggleMembership() {
        if (isAdmin) return;
        if (currentUserId == null) return;
        if (isMember) {
            db.collection("club_members").document(clubId + "_" + currentUserId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        isMember = false;
                        joinClubBtn.setText("Join");
                        loadMembers();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to leave club", Toast.LENGTH_SHORT).show()
                    );
            return;
        }

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    Map<String, Object> member = new HashMap<>();
                    member.put("clubId", clubId);
                    member.put("userId", currentUserId);
                    member.put("userName", userDoc.getString("name"));
                    member.put("profileImage", userDoc.getString("profileImage"));
                    member.put("branch", userDoc.getString("branch"));
                    member.put("enrollment", userDoc.getString("enrollment"));

                    db.collection("club_members")
                            .document(clubId + "_" + currentUserId)
                            .set(member)
                            .addOnSuccessListener(aVoid -> {
                                isMember = true;
                                joinClubBtn.setText("Leave");
                                loadMembers();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to join club", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    private void loadClubEvents() {
        emptyStateText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList, event -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("event_data", event);
            startActivity(intent);
        });
        recyclerView.setAdapter(eventAdapter);

        db.collection("events").whereEqualTo("creatorId", clubId).get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            eventList.add(doc.toObject(Event.class));
                        }
                        eventAdapter.notifyDataSetChanged();
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        emptyStateText.setVisibility(View.VISIBLE);
                        emptyStateText.setText("No events yet");
                    }
                });
    }

    private void loadMembers() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        memberList = new ArrayList<>();
        if (isAdmin) {
            ClubMemberAdminAdapter adminAdapter = new ClubMemberAdminAdapter(memberList, clubId, this::confirmPromote);
            recyclerView.setAdapter(adminAdapter);
        } else {
            memberAdapter = new StudentAdapter(memberList);
            recyclerView.setAdapter(memberAdapter);
        }

        db.collection("users").document(clubId).get()
                .addOnSuccessListener(leaderDoc -> {
                    if (leaderDoc.exists()) {
                        User leader = new User(
                                leaderDoc.getString("name"),
                                leaderDoc.getString("branch"),
                                leaderDoc.getString("enrollment"),
                                leaderDoc.getString("profileImage")
                        );
                        leader.setUid(clubId);
                        memberList.add(leader);
                    }

                    db.collection("club_members").whereEqualTo("clubId", clubId).get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot) {
                                    String userId = doc.getString("userId");
                                    if (clubId.equals(userId)) continue;
                                    User user = new User(
                                            doc.getString("userName"),
                                            doc.getString("branch"),
                                            doc.getString("enrollment"),
                                            doc.getString("profileImage")
                                    );
                                    user.setUid(userId);
                                    memberList.add(user);
                                }

                                if (memberList.isEmpty()) {
                                    recyclerView.setVisibility(View.GONE);
                                    emptyStateText.setVisibility(View.VISIBLE);
                                    emptyStateText.setText("No members yet");
                                } else {
                                    recyclerView.setVisibility(View.VISIBLE);
                                    emptyStateText.setVisibility(View.GONE);
                                }
                                if (recyclerView.getAdapter() != null) {
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                }
                                joinedCount.setText(memberList.size() + " Members");
                            });
                });
    }

    private void confirmPromote(User user) {
        if (user.getUid() == null || user.getUid().equals(clubId)) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Promote to Leader?")
                .setMessage("Promote " + user.getName() + " to lead this club? This will transfer ownership.")
                .setPositiveButton("Promote", (dialog, which) -> promoteToLeader(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promoteToLeader(User newLeader) {
        if (newLeader.getUid() == null) return;
        String newLeaderId = newLeader.getUid();

        db.collection("users").document(clubId).get()
                .addOnSuccessListener(leaderDoc -> {
                    if (!leaderDoc.exists()) return;

                    Map<String, Object> clubFields = new HashMap<>();
                    clubFields.put("role", "leader");
                    clubFields.put("clubName", leaderDoc.getString("clubName"));
                    clubFields.put("clubHandle", leaderDoc.getString("clubHandle"));
                    clubFields.put("clubBio", leaderDoc.getString("clubBio"));
                    clubFields.put("clubLogo", leaderDoc.getString("clubLogo"));
                    clubFields.put("headerImage", leaderDoc.getString("headerImage"));

                    transferClubOwnership(newLeaderId, newLeader, clubFields);
                });
    }

    private void transferClubOwnership(String newLeaderId, User newLeader, Map<String, Object> clubFields) {
        db.collection("events").whereEqualTo("creatorId", clubId).get()
                .addOnSuccessListener(eventsSnapshot -> {
                    WriteBatch batch = db.batch();
                    batch.update(db.collection("users").document(newLeaderId), clubFields);
                    for (DocumentSnapshot doc : eventsSnapshot.getDocuments()) {
                        batch.update(doc.getReference(), "creatorId", newLeaderId);
                    }

                    db.collection("club_members").whereEqualTo("clubId", clubId).get()
                            .addOnSuccessListener(membersSnapshot -> {
                                for (DocumentSnapshot doc : membersSnapshot.getDocuments()) {
                                    String userId = doc.getString("userId");
                                    if (userId == null) continue;
                                    Map<String, Object> data = new HashMap<>(doc.getData() != null ? doc.getData() : new HashMap<>());
                                    data.put("clubId", newLeaderId);
                                    batch.set(db.collection("club_members").document(newLeaderId + "_" + userId), data);
                                    batch.delete(doc.getReference());
                                }

                                Map<String, Object> leaderMember = new HashMap<>();
                                leaderMember.put("clubId", newLeaderId);
                                leaderMember.put("userId", newLeaderId);
                                leaderMember.put("userName", newLeader.getName());
                                leaderMember.put("profileImage", newLeader.getProfileImage());
                                leaderMember.put("branch", newLeader.getBranch());
                                leaderMember.put("enrollment", newLeader.getEnrollment());
                                batch.set(db.collection("club_members").document(newLeaderId + "_" + newLeaderId), leaderMember);

                                batch.update(db.collection("users").document(clubId), "role", "user");

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Leader updated", Toast.LENGTH_SHORT).show();
                                            AdminAuditLogger.log("CLUB_TRANSFER_LEADER", "club", clubId,
                                                    clubId, newLeaderId);
                                            finish();
                                            ClubDetailsActivity.start(this, newLeaderId);
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Transfer failed", Toast.LENGTH_SHORT).show()
                                        );
                            });
                });
    }
}
