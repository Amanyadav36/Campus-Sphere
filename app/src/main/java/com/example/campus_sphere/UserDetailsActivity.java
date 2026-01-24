package com.example.campus_sphere;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// Cloudinary Imports
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

// Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {

    // --- CONFIGURATION: REPLACE THESE WITH YOUR CLOUDINARY DETAILS ---
    private static final String CLOUD_NAME = "dpadbarxt";
    private static final String UPLOAD_PRESET = "campus_sphere_preset";
    // ----------------------------------------------------------------

    // UI Elements
    private ImageView profileImage;
    private EditText nameInput, enrollmentInput, interestInput;
    private Spinner genderSpinner, branchSpinner, sectionSpinner;
    private Button submitBtn;

    // Data
    private Uri selectedImageUri = null;

    // 1. IMAGE PICKER LAUNCHER
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profileImage.setImageURI(uri); // Show the selected image immediately
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // --- 1. INITIALIZE CLOUDINARY (SAFE MODE) ---
        // This prevents crashes if the app tries to init Cloudinary twice
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("secure", true);
            MediaManager.init(getApplicationContext(), config);
        } catch (IllegalStateException e) {
            // Cloudinary is already initialized; we can ignore this error.
            Log.d("Cloudinary", "Already initialized");
        }

        // Initialize Views
        profileImage = findViewById(R.id.profileImage);
        nameInput = findViewById(R.id.name);
        enrollmentInput = findViewById(R.id.enrollment);
        interestInput = findViewById(R.id.interest);
        genderSpinner = findViewById(R.id.genderSpinner);
        branchSpinner = findViewById(R.id.branchSpinner);
        sectionSpinner = findViewById(R.id.sectionSpinner);
        submitBtn = findViewById(R.id.submitBtn);

        // Setup Spinners
        setupSpinners();

        // Load existing data if editing
        loadExistingData();

        // 2. CLICK IMAGE TO OPEN GALLERY
        profileImage.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        // 3. CLICK SAVE BUTTON
        submitBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadToCloudinary();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Gender", "Male", "Female", "Other"});
        genderSpinner.setAdapter(genderAdapter);

        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Branch", "CSE", "IT", "Mechanical", "Civil", "ECE"});
        branchSpinner.setAdapter(branchAdapter);

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Section", "A", "B", "C", "D"});
        sectionSpinner.setAdapter(sectionAdapter);
    }

    // --- 4. CLOUDINARY UPLOAD LOGIC ---
    private void uploadToCloudinary() {
        // SAFETY CHECK: Ensure user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // CASE 1: No new image selected -> Just save text
        if (selectedImageUri == null) {
            saveDataToFirestore(null, progressDialog);
            return;
        }

        // CASE 2: Upload Image to Cloudinary
        MediaManager.get().upload(selectedImageUri)
                .unsigned(UPLOAD_PRESET) // Must match your Cloudinary setting
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Optional: Update progress bar here
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Optional: Show percentage
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Upload success! Get the web URL
                        String downloadUrl = (String) resultData.get("secure_url");
                        saveDataToFirestore(downloadUrl, progressDialog);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(UserDetailsActivity.this,
                                "Upload Error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Upload rescheduled (e.g., no internet)
                    }
                })
                .dispatch();
    }


    private void saveDataToFirestore(String imageUrl, ProgressDialog progressDialog) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", nameInput.getText().toString().trim());
        userMap.put("enrollment", enrollmentInput.getText().toString().trim());
        userMap.put("gender", genderSpinner.getSelectedItem().toString());
        userMap.put("branch", branchSpinner.getSelectedItem().toString());
        userMap.put("section", sectionSpinner.getSelectedItem().toString());
        userMap.put("interest", interestInput.getText().toString().trim());

        if (imageUrl != null) {
            userMap.put("profileImage", imageUrl);
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(UserDetailsActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();


                    boolean isNewUser = getIntent().getBooleanExtra("is_new_user", false);

                    if (isNewUser) {
                        // NEW USER: Go to Home Page (MainActivity) and clear history
                        Intent intent = new Intent(UserDetailsActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {

                        finish();
                    }

                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(UserDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs() {
        if (nameInput.getText().toString().isEmpty()) {
            nameInput.setError("Name is required");
            return false;
        }
        return true;
    }

    private void loadExistingData() {
        Intent intent = getIntent();
        if(intent.hasExtra("name")) {
            nameInput.setText(intent.getStringExtra("name"));
            enrollmentInput.setText(intent.getStringExtra("enrollment"));
            interestInput.setText(intent.getStringExtra("interest"));
            // Note: Setting Spinner selection by text requires a loop or helper function
            // keeping it simple for now.
        }
    }
}