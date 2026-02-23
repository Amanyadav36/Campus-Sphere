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

import com.bumptech.glide.Glide; // ✅ Added Glide to show existing profile image
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {

    private static final String CLOUD_NAME = "dpadbarxt";
    private static final String UPLOAD_PRESET = "campus_sphere_preset";

    // UI Elements
    private ImageView profileImage;
    private EditText nameInput, emailInput, mobileInput, bioInput, enrollmentInput, interestInput;
    private Spinner branchSpinner, yearSpinner;
    private Button submitBtn;

    // Data
    private Uri selectedImageUri = null;
    private String currentRole = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profileImage.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // --- 1. Safe Cloudinary Init ---
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("secure", true);
            MediaManager.init(getApplicationContext(), config);
        } catch (IllegalStateException e) {
            Log.d("Cloudinary", "Already initialized");
        }

        // Initialize Views
        profileImage = findViewById(R.id.profileImage);
        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        mobileInput = findViewById(R.id.mobile);
        bioInput = findViewById(R.id.bio);
        enrollmentInput = findViewById(R.id.enrollment);
        interestInput = findViewById(R.id.interest);
        branchSpinner = findViewById(R.id.branchSpinner);
        yearSpinner = findViewById(R.id.yearSpinner);
        
        submitBtn = findViewById(R.id.submitBtn);

        setupSpinners();
        loadExistingData(); // ✅ Load data (including Spinners)

        profileImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        submitBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadToCloudinary();
            }
        });
    }

    private void setupSpinners() {
        branchSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Select Branch", "CSE", "IT", "Mechanical", "Civil", "ECE"}));
        yearSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Select Year", "1st Year", "2nd Year", "3rd Year", "4th Year"}));
    }

    private void uploadToCloudinary() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Profile...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (selectedImageUri == null) {
            saveDataToFirestore(null, progressDialog);
            return;
        }

        // ✅ Run Cloudinary Upload
        MediaManager.get().upload(selectedImageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String downloadUrl = (String) resultData.get("secure_url");
                        // ✅ Fix: Move to Main Thread before calling Firestore/UI
                        runOnUiThread(() -> saveDataToFirestore(downloadUrl, progressDialog));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(UserDetailsActivity.this, "Upload Error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        });
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveDataToFirestore(String imageUrl, ProgressDialog progressDialog) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String nameStr = nameInput.getText().toString().trim();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", nameStr);
        userMap.put("enrollment", enrollmentInput.getText().toString().trim());
        userMap.put("branch", branchSpinner.getSelectedItem().toString());
        userMap.put("year", yearSpinner.getSelectedItem().toString());
        userMap.put("interest", interestInput.getText().toString().trim());
        userMap.put("bio", bioInput.getText().toString().trim());
        userMap.put("mobile", mobileInput.getText().toString().trim());
        
        userMap.put("profileCompleted", true);

        String writtenEmail = emailInput.getText().toString().trim();
        if (!writtenEmail.isEmpty()) {
             userMap.put("email", writtenEmail);
        } else if (email != null) {
             userMap.put("email", email);
        }
        
        if (currentRole == null || currentRole.isEmpty()) userMap.put("role", "user");
        if (imageUrl != null) userMap.put("profileImage", imageUrl);

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Update Auth Profile (Critical for Email Templates)
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nameStr)
                            .build();
                    FirebaseAuth.getInstance().getCurrentUser().updateProfile(profileUpdates);

                    progressDialog.dismiss();
                    Toast.makeText(UserDetailsActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();

                    // Navigate
                    if (currentRole == null) {
                        Intent intent = new Intent(UserDetailsActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(UserDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Load Text Fields
                        nameInput.setText(document.getString("name"));
                        emailInput.setText(document.getString("email"));
                        mobileInput.setText(document.getString("mobile"));
                        bioInput.setText(document.getString("bio"));
                        enrollmentInput.setText(document.getString("enrollment"));
                        interestInput.setText(document.getString("interest"));
                        
                        // ✅ Load Spinners
                        setSpinnerSelection(branchSpinner, document.getString("branch"));
                        
                        String year = document.getString("year");
                        if (year == null) year = document.getString("section"); // fallback for old data
                        setSpinnerSelection(yearSpinner, year);

                        // Load Image if exists
                        String imgUrl = document.getString("profileImage");
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            Glide.with(this).load(imgUrl).into(profileImage);
                        }

                        currentRole = document.getString("role");
                    }
                });
    }

    // ✅ Helper method to select correct spinner item
    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    private boolean validateInputs() {
        String nameStr = nameInput.getText().toString().trim();
        String emailStr = emailInput.getText().toString().trim();
        String mobileStr = mobileInput.getText().toString().trim();

        if (nameStr.isEmpty()) {
            nameInput.setError("Name is required");
            nameInput.requestFocus();
            return false;
        }

        if (!nameStr.matches("^[a-zA-Z\\s]+$")) {
            nameInput.setError("Name should only contain letters and spaces (no special characters)");
            nameInput.requestFocus();
            return false;
        }

        if (emailStr.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return false;
        }

        if (mobileStr.isEmpty()) {
            mobileInput.setError("Phone number is required");
            mobileInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.PHONE.matcher(mobileStr).matches() || mobileStr.length() < 10) {
            mobileInput.setError("Please enter a valid phone number");
            mobileInput.requestFocus();
            return false;
        }

        return true;
    }
}