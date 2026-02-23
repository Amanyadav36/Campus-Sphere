package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // UI Variables
    private TextView profileName, profileEmail, profileEnrollment, profileBranch, profileYear, profileInterest, profileBio, profileMobile;
    private TextView profileEvents, profileBookmarks, profileCertificates, profileReceipts;
    private ImageView profileImageDisplay;
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
        profileYear = view.findViewById(R.id.profileYear);
        profileInterest = view.findViewById(R.id.profileInterest);
        profileBio = view.findViewById(R.id.profileBio);
        profileMobile = view.findViewById(R.id.profileMobile);
        profileEvents = view.findViewById(R.id.profileEvents);
        profileBookmarks = view.findViewById(R.id.profileBookmarks);
        profileCertificates = view.findViewById(R.id.profileCertificates);
        profileReceipts = view.findViewById(R.id.profileReceipts);
        profileImageDisplay = view.findViewById(R.id.profileImageDisplay);

        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        // 2. Button Actions
        editProfileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserDetailsActivity.class);
            // OPTIONAL: Pass existing data so user doesn't have to re-type
            intent.putExtra("is_new_user", false);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(v -> logout());

        // Note: We removed loadUserData() from here because onResume() handles it now.

        // 3. Section Navigation
        view.findViewById(R.id.sectionRegisteredEvents).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ListSectionActivity.class);
            intent.putExtra("SECTION_TITLE", "Registered Events");
            startActivity(intent);
        });
        
        view.findViewById(R.id.sectionBookmarks).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ListSectionActivity.class);
            intent.putExtra("SECTION_TITLE", "Bookmarks");
            startActivity(intent);
        });
        
        view.findViewById(R.id.sectionCertificates).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ListSectionActivity.class);
            intent.putExtra("SECTION_TITLE", "Certificates");
            startActivity(intent);
        });
        
        view.findViewById(R.id.sectionReceipts).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ListSectionActivity.class);
            intent.putExtra("SECTION_TITLE", "Payment Receipts");
            startActivity(intent);
        });

        return view;
    }

    // --- CRITICAL FIX: REFRESH DATA ON RETURN ---
    // This ensures that when you come back from "Edit Profile", the new photo shows up instantly.
    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }
    // --------------------------------------------

    private void loadUserData() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        if (profileEmail != null) {
            profileEmail.setText(email != null ? email : "No Email");
        }

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    // Check isAdded() to prevent crash if user left the screen quickly
                    if (document.exists() && isAdded()) {
                        String name = document.getString("name");
                        String branch = document.getString("branch");
                        String year = document.getString("year");
                        if (year == null) year = document.getString("section");
                        String enroll = document.getString("enrollment");
                        String interest = document.getString("interest");
                        String bio = document.getString("bio");
                        String mobile = document.getString("mobile");
                        String imageUrl = document.getString("profileImage");
                        
                        String events = document.getString("events_count");
                        String bookmarks = document.getString("bookmarks_count");
                        String certificates = document.getString("certificates_count");
                        String receipts = document.getString("receipts_count");

                        if (profileName != null) profileName.setText(name != null ? name : "Student Name");
                        if (profileEnrollment != null) profileEnrollment.setText(enroll != null ? enroll : "-");
                        if (profileBranch != null) profileBranch.setText(branch != null ? branch : "-");
                        if (profileYear != null) profileYear.setText(year != null ? year : "-");
                        if (profileInterest != null) profileInterest.setText(interest != null ? interest : "-");
                        if (profileBio != null) profileBio.setText(bio != null ? bio : "I am an enthusiastic student.");
                        if (profileMobile != null) profileMobile.setText(mobile != null ? mobile : "-");

                        if (profileEvents != null) profileEvents.setText(events != null && !events.isEmpty() ? events : "0");
                        if (profileBookmarks != null) profileBookmarks.setText(bookmarks != null && !bookmarks.isEmpty() ? bookmarks : "0");
                        if (profileCertificates != null) profileCertificates.setText(certificates != null && !certificates.isEmpty() ? certificates : "0");
                        if (profileReceipts != null) profileReceipts.setText(receipts != null && !receipts.isEmpty() ? receipts : "0");

                        // LOAD IMAGE
                        if (imageUrl != null && !imageUrl.isEmpty() && profileImageDisplay != null) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(android.R.drawable.sym_def_app_icon)
                                    .error(android.R.drawable.stat_notify_error)
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
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient googleClient = GoogleSignIn.getClient(requireActivity(), gso);
            googleClient.signOut();
        } catch (Exception e) {
            // Ignore Google errors
        }

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}