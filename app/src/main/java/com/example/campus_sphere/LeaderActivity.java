package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class LeaderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader); // We will create this XML next

        BottomNavigationView bottomNav = findViewById(R.id.leader_bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        // Load Dashboard by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.leader_fragment_container, new LeaderHomeFragment())
                    .commit();
        }
    }

    private final BottomNavigationView.OnItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_leader_home) {
            selectedFragment = new LeaderHomeFragment();
        } else if (itemId == R.id.nav_leader_create) {
            selectedFragment = new CreateEventFragment();
        } else if (itemId == R.id.nav_leader_manage) {
            selectedFragment = new ManageClubFragment();
        } else if (itemId == R.id.nav_leader_profile) {
            selectedFragment = new ProfileFragment(); // Re-use the same profile page
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.leader_fragment_container, selectedFragment)
                    .commit();
        }
        return true;
    };
}