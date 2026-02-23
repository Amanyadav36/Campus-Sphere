package com.example.campus_sphere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ManageClubFragment extends Fragment {

    // Views
    private TextView clubNameTitle, clubHandle, clubBio, joinedCount, emptyStateText;
    private ImageView headerImage;
    private CircleImageView clubIcon;
    private Button editProfileBtn;
    private Button joinClubBtn;
    private TextView coverHint;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;

    // Data & Firebase
    private FirebaseFirestore db;
    private String currentUid;
    private boolean isUploadingHeader = false; // TRUE = Header, FALSE = Club Logo

    // Adapters
    private EventAdapter eventAdapter;
    private StudentAdapter memberAdapter;
    private List<Event> eventList;
    private List<User> memberList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_club, container, false);

        // --- ✅ FIX: SAFELY INITIALIZE CLOUDINARY HERE ---
        // This prevents the crash when running inside LeaderActivity
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", "dpadbarxt");
                config.put("secure", true);
                if (getContext() != null) {
                    MediaManager.init(getContext().getApplicationContext(), config);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        // -------------------------------------------------

        // 1. Initialize
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        clubNameTitle = view.findViewById(R.id.clubNameTitle);
        clubHandle = view.findViewById(R.id.clubHandle);
        clubBio = view.findViewById(R.id.clubBio);
        joinedCount = view.findViewById(R.id.joinedCount);
        headerImage = view.findViewById(R.id.headerImage);
        clubIcon = view.findViewById(R.id.clubIcon);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        joinClubBtn = view.findViewById(R.id.joinClubBtn);
        coverHint = view.findViewById(R.id.coverHint);
        recyclerView = view.findViewById(R.id.clubContentRecycler);
        tabLayout = view.findViewById(R.id.clubTabLayout);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        // 2. SETUP DEFAULT LAYOUT MANAGER (Crucial Fix)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Load Data
        loadClubData();
        loadMyEvents(); // Default view

        // 4. Image Upload Logic
        ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        uploadToCloudinary(result.getData().getData());
                    }
                }
        );

        // Header Click -> Upload Header
        headerImage.setOnClickListener(v -> {
            isUploadingHeader = true;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePicker.launch(intent);
        });

        // Icon Click -> Upload Club Logo (Separate from Profile Pic)
        clubIcon.setOnClickListener(v -> {
            isUploadingHeader = false;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePicker.launch(intent);
        });

        editProfileBtn.setOnClickListener(v -> showEditDialog());
        joinClubBtn.setVisibility(View.GONE);
        coverHint.setVisibility(View.VISIBLE);

        // 5. Tab Logic
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) { // Members
                    loadMembers();
                } else if (tab.getPosition() == 1) { // Media
                    recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                    loadMyEvents();
                } else { // Events (Default)
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    loadMyEvents();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void loadClubData() {
        if (getContext() == null) return; // Safety check

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("clubName");
                        String handle = document.getString("clubHandle");
                        String bio = document.getString("clubBio");

                        // FETCHING SEPARATE IMAGES
                        String headerUrl = document.getString("headerImage");
                        String logoUrl = document.getString("clubLogo");

                        if (name != null) clubNameTitle.setText(name);
                        if (handle != null) clubHandle.setText("@" + handle);
                        if (bio != null) clubBio.setText(bio);

                        // Load Images
                        if (getContext() != null) {
                            if (headerUrl != null && !headerUrl.isEmpty()) {
                                Glide.with(this).load(headerUrl).centerCrop().into(headerImage);
                            }
                            if (logoUrl != null && !logoUrl.isEmpty()) {
                                Glide.with(this).load(logoUrl).into(clubIcon);
                            }
                        }

                        ensureLeaderMembership(document);
                    }
                });
    }

    private void ensureLeaderMembership(DocumentSnapshot leaderDoc) {
        Map<String, Object> member = new HashMap<>();
        member.put("clubId", currentUid);
        member.put("userId", currentUid);
        member.put("userName", leaderDoc.getString("name"));
        member.put("profileImage", leaderDoc.getString("profileImage"));
        member.put("branch", leaderDoc.getString("branch"));
        member.put("enrollment", leaderDoc.getString("enrollment"));

        db.collection("club_members")
                .document(currentUid + "_" + currentUid)
                .set(member);
    }

    private void updateImageInFirestore(String url) {
        // ✅ Save to distinct fields
        String field = isUploadingHeader ? "headerImage" : "clubLogo";

        db.collection("users").document(currentUid).update(field, url)
                .addOnSuccessListener(aVoid -> {
                    loadClubData(); // Refresh UI
                    String msg = isUploadingHeader ? "Header Updated" : "Club Logo Updated";
                    if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) Toast.makeText(getContext(), "Update Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadToCloudinary(Uri uri) {
        if (getContext() != null) Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(uri)
                .unsigned("campus_sphere_preset")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        updateImageInFirestore(url);
                    }
                    @Override public void onError(String requestId, ErrorInfo errorInfo) {
                        if (getContext() != null) Toast.makeText(getContext(), "Upload Error: " + errorInfo.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                }).dispatch();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Club Details");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(getContext());
        nameInput.setHint("Club Name");
        nameInput.setText(clubNameTitle.getText().toString());
        layout.addView(nameInput);

        final EditText handleInput = new EditText(getContext());
        handleInput.setHint("Handle");
        handleInput.setText(clubHandle.getText().toString().replace("@", ""));
        layout.addView(handleInput);

        final EditText bioInput = new EditText(getContext());
        bioInput.setHint("Bio");
        bioInput.setText(clubBio.getText().toString());
        layout.addView(bioInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("clubName", nameInput.getText().toString());
            updates.put("clubHandle", handleInput.getText().toString());
            updates.put("clubBio", bioInput.getText().toString());

            db.collection("users").document(currentUid).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        loadClubData();
                        Toast.makeText(getContext(), "Saved!", Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadMyEvents() {
        emptyStateText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        eventList = new ArrayList<>();

        // ✅ FIX: Pass the Click Listener to the Adapter
        eventAdapter = new EventAdapter(eventList, event -> {
            // When a leader clicks their own event, open the details page
            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
            intent.putExtra("event_data", event);
            startActivity(intent);
        });

        recyclerView.setAdapter(eventAdapter);

        db.collection("events").whereEqualTo("creatorId", currentUid).get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            eventList.add(doc.toObject(Event.class));
                        }
                        eventAdapter.notifyDataSetChanged();
                    } else {
                        // Handle empty state if needed
                        recyclerView.setVisibility(View.GONE);
                        emptyStateText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadMembers() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        memberList = new ArrayList<>();
        memberAdapter = new StudentAdapter(memberList);
        recyclerView.setAdapter(memberAdapter);

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(leaderDoc -> {
                    if (leaderDoc.exists()) {
                        User leader = new User(
                                leaderDoc.getString("name"),
                                leaderDoc.getString("branch"),
                                leaderDoc.getString("enrollment"),
                                leaderDoc.getString("profileImage")
                        );
                        memberList.add(leader);
                    }
                    db.collection("club_members").whereEqualTo("clubId", currentUid).get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot) {
                                    String userId = doc.getString("userId");
                                    if (currentUid.equals(userId)) continue;
                                    User user = new User(
                                            doc.getString("userName"),
                                            doc.getString("branch"),
                                            doc.getString("enrollment"),
                                            doc.getString("profileImage")
                                    );
                                    memberList.add(user);
                                }

                                if (memberList.isEmpty()) {
                                    recyclerView.setVisibility(View.GONE);
                                    emptyStateText.setVisibility(View.VISIBLE);
                                } else {
                                    recyclerView.setVisibility(View.VISIBLE);
                                    emptyStateText.setVisibility(View.GONE);
                                }
                                memberAdapter.notifyDataSetChanged();
                                joinedCount.setText(memberList.size() + " Members");
                            });
                });
    }
}
