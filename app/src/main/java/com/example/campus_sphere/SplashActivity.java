package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeFromSplash, SPLASH_DELAY_MS);
    }

    private void routeFromSplash() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        startActivity(new Intent(this, UserDetailsActivity.class));
                        finish();
                        return;
                    }

                    Boolean profileCompleted = document.getBoolean("profileCompleted");
                    if (profileCompleted == null || !profileCompleted) {
                        startActivity(new Intent(this, UserDetailsActivity.class));
                        finish();
                        return;
                    }

                    String role = document.getString("role");
                    navigateToDashboard(role);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if (role == null) role = "user";

        if ("admin".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else if ("leader".equals(role)) {
            intent = new Intent(this, LeaderActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
