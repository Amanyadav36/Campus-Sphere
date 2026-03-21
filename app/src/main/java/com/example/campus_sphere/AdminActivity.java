package com.example.campus_sphere;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        bottomNav = findViewById(R.id.admin_bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_admin_home) {
                selected = new AdminHomeFragment();
            } else if (itemId == R.id.nav_admin_actions) {
                selected = new AdminActionFragment();
            } else if (itemId == R.id.nav_admin_profile) {
                ProfileFragment f = new ProfileFragment();
                Bundle args = new Bundle();
                args.putString(ProfileFragment.ARG_MODE, ProfileFragment.MODE_ADMIN);
                f.setArguments(args);
                selected = f;
            }

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.admin_fragment_container, selected)
                        .commit();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_admin_home);
        }
    }

    public void selectTab(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }
}
