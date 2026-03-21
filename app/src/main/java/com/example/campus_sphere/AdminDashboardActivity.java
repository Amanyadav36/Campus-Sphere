package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button logoutBtn;
    private Button manageUsersBtn;
    private Button moderateEventsBtn;
    private Button reviewPaymentsBtn;
    private Button analyticsBtn;

    private TextView usersCount;
    private TextView eventsCount;
    private TextView ticketsCount;
    private TextView revenueCount;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        logoutBtn = findViewById(R.id.adminLogoutBtn);
        manageUsersBtn = findViewById(R.id.btnManageUsers);
        moderateEventsBtn = findViewById(R.id.btnModerateEvents);
        reviewPaymentsBtn = findViewById(R.id.btnReviewPayments);
        analyticsBtn = findViewById(R.id.btnAdminAnalytics);

        usersCount = findViewById(R.id.adminUsersCount);
        eventsCount = findViewById(R.id.adminEventsCount);
        ticketsCount = findViewById(R.id.adminTicketsCount);
        revenueCount = findViewById(R.id.adminRevenueCount);

        logoutBtn.setOnClickListener(v -> logoutAdmin());
        manageUsersBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminUsersActivity.class)));
        moderateEventsBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminEventsActivity.class)));
        reviewPaymentsBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminPaymentsActivity.class)));
        if (analyticsBtn != null) {
            analyticsBtn.setOnClickListener(v -> startActivity(new Intent(this, AdminAnalyticsActivity.class)));
        }

        loadStats();
    }

    private void loadStats() {
        db.collection("users").get()
                .addOnSuccessListener(snapshot -> usersCount.setText(String.valueOf(snapshot.size())));

        db.collection("events").get()
                .addOnSuccessListener(snapshot -> eventsCount.setText(String.valueOf(snapshot.size())));

        db.collection("tickets").get()
                .addOnSuccessListener(snapshot -> ticketsCount.setText(String.valueOf(snapshot.size())));

        db.collection("tickets").get()
                .addOnSuccessListener(snapshot -> {
                    long paidCount = 0;
                    for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                        String paymentId = snapshot.getDocuments().get(i).getString("paymentId");
                        if (paymentId != null && !paymentId.equals("FREE_TICKET")) {
                            paidCount++;
                        }
                    }
                    revenueCount.setText("â‚¹" + paidCount);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void logoutAdmin() {
        auth.signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleClient = GoogleSignIn.getClient(this, gso);

        googleClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(AdminDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
