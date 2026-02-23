package com.example.campus_sphere;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminCreateClubActivity extends AppCompatActivity {

    private ImageView headerPreview;
    private CircleImageView logoPreview;
    private Button headerBtn;
    private Button logoBtn;
    private Button createBtn;
    private EditText emailInput;
    private EditText nameInput;
    private EditText handleInput;
    private EditText bioInput;

    private Uri headerUri;
    private Uri logoUri;
    private String headerUrl;
    private String logoUrl;

    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> headerPicker;
    private ActivityResultLauncher<Intent> logoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_club);

        db = FirebaseFirestore.getInstance();
        initCloudinarySafe();

        headerPreview = findViewById(R.id.clubHeaderPreview);
        logoPreview = findViewById(R.id.clubLogoPreview);
        headerBtn = findViewById(R.id.clubHeaderBtn);
        logoBtn = findViewById(R.id.clubLogoBtn);
        createBtn = findViewById(R.id.createClubBtn);
        emailInput = findViewById(R.id.clubLeaderEmail);
        nameInput = findViewById(R.id.clubNameInput);
        handleInput = findViewById(R.id.clubHandleInput);
        bioInput = findViewById(R.id.clubBioInput);

        headerPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        headerUri = result.getData().getData();
                        Glide.with(this).load(headerUri).centerCrop().into(headerPreview);
                    }
                }
        );

        logoPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        logoUri = result.getData().getData();
                        Glide.with(this).load(logoUri).circleCrop().into(logoPreview);
                    }
                }
        );

        headerBtn.setOnClickListener(v -> pickImage(headerPicker));
        logoBtn.setOnClickListener(v -> pickImage(logoPicker));
        createBtn.setOnClickListener(v -> validateAndCreate());
    }

    private void pickImage(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        launcher.launch(intent);
    }

    private void initCloudinarySafe() {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", "dpadbarxt");
                config.put("secure", true);
                MediaManager.init(getApplicationContext(), config);
            } catch (Exception ex) {
                Log.e("Cloudinary", "Init failed", ex);
            }
        }
    }

    private void validateAndCreate() {
        String email = emailInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String handle = handleInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();

        if (email.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Email and club name are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (headerUri == null || logoUri == null) {
            Toast.makeText(this, "Upload club icon and background", Toast.LENGTH_SHORT).show();
            return;
        }

        createBtn.setEnabled(false);
        createBtn.setText("Uploading...");

        uploadImage(headerUri, new ImageUploadCallback() {
            @Override
            public void onSuccess(String url) {
                headerUrl = url;
                uploadImage(logoUri, new ImageUploadCallback() {
                    @Override
                    public void onSuccess(String logo) {
                        logoUrl = logo;
                        createClub(email, name, handle, bio);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(AdminCreateClubActivity.this, message, Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminCreateClubActivity.this, message, Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void uploadImage(Uri uri, ImageUploadCallback callback) {
        MediaManager.get().upload(uri)
                .unsigned("campus_sphere_preset")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        callback.onSuccess(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo errorInfo) {
                        callback.onError("Upload failed: " + errorInfo.getDescription());
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                })
                .dispatch();
    }

    private interface ImageUploadCallback {
        void onSuccess(String url);
        void onError(String message);
    }

    private void createClub(String email, String name, String handle, String bio) {
        db.collection("users").whereEqualTo("clubName", name).get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        Toast.makeText(this, "Club name already exists", Toast.LENGTH_SHORT).show();
                        resetButton();
                        return;
                    }

                    db.collection("users").whereEqualTo("email", email).get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.isEmpty()) {
                                    Toast.makeText(this, "No user found with that email", Toast.LENGTH_SHORT).show();
                                    resetButton();
                                    return;
                                }
                                DocumentSnapshot doc = snapshot.getDocuments().get(0);
                                String userId = doc.getId();
                                String existingRole = doc.getString("role");
                                if ("leader".equals(existingRole) && doc.getString("clubName") != null) {
                                    Toast.makeText(this, "User already leads a club", Toast.LENGTH_SHORT).show();
                                    resetButton();
                                    return;
                                }

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("role", "leader");
                                updates.put("clubName", name);
                                updates.put("clubHandle", handle);
                                updates.put("clubBio", bio);
                                updates.put("clubLogo", logoUrl);
                                updates.put("headerImage", headerUrl);

                                db.collection("users").document(userId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            ensureLeaderMembership(userId, doc);
                                            AdminAuditLogger.log("CREATE_CLUB", "club", userId, "", name);
                                            Toast.makeText(this, "Club created", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to create club", Toast.LENGTH_SHORT).show();
                                            resetButton();
                                        });
                            });
                });
    }

    private void ensureLeaderMembership(String clubId, DocumentSnapshot leaderDoc) {
        Map<String, Object> member = new HashMap<>();
        member.put("clubId", clubId);
        member.put("userId", clubId);
        member.put("userName", leaderDoc.getString("name"));
        member.put("profileImage", leaderDoc.getString("profileImage"));
        member.put("branch", leaderDoc.getString("branch"));
        member.put("enrollment", leaderDoc.getString("enrollment"));

        db.collection("club_members")
                .document(clubId + "_" + clubId)
                .set(member);
    }

    private void resetButton() {
        createBtn.setEnabled(true);
        createBtn.setText("Create Club");
    }
}
