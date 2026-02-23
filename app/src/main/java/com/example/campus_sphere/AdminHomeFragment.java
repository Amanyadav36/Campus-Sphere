package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeFragment extends Fragment {

    private Button logoutBtn;
    private Button manageUsersBtn;
    private Button moderateEventsBtn;
    private Button reviewPaymentsBtn;
    private TextView usersCount;
    private TextView eventsCount;
    private TextView ticketsCount;
    private TextView revenueCount;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_admin_dashboard, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        logoutBtn = view.findViewById(R.id.adminLogoutBtn);
        manageUsersBtn = view.findViewById(R.id.btnManageUsers);
        moderateEventsBtn = view.findViewById(R.id.btnModerateEvents);
        reviewPaymentsBtn = view.findViewById(R.id.btnReviewPayments);
        usersCount = view.findViewById(R.id.adminUsersCount);
        eventsCount = view.findViewById(R.id.adminEventsCount);
        ticketsCount = view.findViewById(R.id.adminTicketsCount);
        revenueCount = view.findViewById(R.id.adminRevenueCount);

        logoutBtn.setOnClickListener(v -> logoutAdmin());
        manageUsersBtn.setOnClickListener(v -> switchTab(R.id.nav_admin_users));
        moderateEventsBtn.setOnClickListener(v -> switchTab(R.id.nav_admin_events));
        reviewPaymentsBtn.setOnClickListener(v -> switchTab(R.id.nav_admin_payments));

        loadStats();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void switchTab(int itemId) {
        if (getActivity() instanceof AdminActivity) {
            ((AdminActivity) getActivity()).selectTab(itemId);
        }
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
                    revenueCount.setText("₹" + paidCount);
                });
    }

    private void logoutAdmin() {
        auth.signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleClient = GoogleSignIn.getClient(requireContext(), gso);

        googleClient.signOut().addOnCompleteListener(task -> {
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}
