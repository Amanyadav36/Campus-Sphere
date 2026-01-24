package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView; // Imported ImageView
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide; // Imported Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // UI Variables
    private TextView profileName, profileEmail, profileEnrollment, profileBranch, profileSection, profileInterest;
    private ImageView profileImageDisplay; // Added Image Variable
    private Button editProfileBtn, logoutBtn;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile_fixed, container, false);

        auth = FirebaseAuth.getInstance();

        // 1. Initialize Views
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileEnrollment = view.findViewById(R.id.profileEnrollment);
        profileBranch = view.findViewById(R.id.profileBranch);
        profileSection = view.findViewById(R.id.profileSection);
        profileInterest = view.findViewById(R.id.profileInterest);

        // Initialize Image View
        profileImageDisplay = view.findViewById(R.id.profileImageDisplay);

        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        // 2. Load Data
        loadUserData();

        // 3. Button Actions
        editProfileBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UserDetailsActivity.class));
        });

        logoutBtn.setOnClickListener(v -> logout());

        return view;
    }

    private void loadUserData() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        if (profileEmail != null) {
            profileEmail.setText(email != null ? email : "No Email");
        }

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String branch = document.getString("branch");
                        String section = document.getString("section");
                        String enroll = document.getString("enrollment");
                        String interest = document.getString("interest");
                        String imageUrl = document.getString("profileImage"); // Fetch URL

                        if (profileName != null) profileName.setText(name != null ? name : "Student");
                        if (profileEnrollment != null) profileEnrollment.setText(enroll != null ? enroll : "-");
                        if (profileBranch != null) profileBranch.setText(branch != null ? branch : "-");
                        if (profileSection != null) profileSection.setText(section != null ? section : "-");
                        if (profileInterest != null) profileInterest.setText(interest != null ? interest : "-");

                        // LOAD IMAGE USING GLIDE
                        if (imageUrl != null && !imageUrl.isEmpty() && profileImageDisplay != null) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(android.R.drawable.sym_def_app_icon) // Show this while loading
                                    .error(android.R.drawable.stat_notify_error)      // Show this if error
                                    .into(profileImageDisplay);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        auth.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleClient = GoogleSignIn.getClient(requireActivity(), gso);
        googleClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}