package com.example.campus_sphere;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ❌ OLD: public class EventDetailsActivity extends AppCompatActivity implements PaymentResultListener
// ✅ NEW: Remove the listener interface
public class EventDetailsActivity extends AppCompatActivity {

    // UI Components
    private TextView title, price, date, venue, desc;
    private ImageView image;
    private Button actionBtn, viewStudentsBtn;

    // Data
    private Event event;
    private FirebaseFirestore db;
    private String currentUid;

    // ✅ NEW: Launcher to handle result from PaymentMethodActivity
    private ActivityResultLauncher<Intent> paymentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Initialize Views
        title = findViewById(R.id.detailTitle);
        price = findViewById(R.id.detailPrice);
        date = findViewById(R.id.detailDateTime);
        venue = findViewById(R.id.detailVenue);
        desc = findViewById(R.id.detailDesc);
        image = findViewById(R.id.detailImage);
        actionBtn = findViewById(R.id.actionBtn);
        viewStudentsBtn = findViewById(R.id.viewStudentsBtn);

        // ✅ NEW: Register the Payment Launcher
        // This waits for PaymentMethodActivity to finish and give back the Payment ID
        paymentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Payment Successful! Get the ID and save to DB
                        String paymentId = result.getData().getStringExtra("payment_id");
                        registerUser(paymentId);
                    } else {
                        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 3. Get Event Data
        event = (Event) getIntent().getSerializableExtra("event_data");

        if (event != null) {
            setupUI();
            checkUserRole();
            checkIfAlreadyRegistered();
        }
    }

    private void setupUI() {
        title.setText(event.getTitle());
        desc.setText(event.getDescription());
        date.setText(event.getDate() + " at " + event.getTime());
        venue.setText(event.getVenue());

        if (event.getImageUrl() != null) {
            Glide.with(this).load(event.getImageUrl()).into(image);
        }

        // Price Logic
        long amount = event.getAmountInPaise();
        if (amount > 0) {
            price.setText("₹ " + event.getPrice());
            actionBtn.setText("Register Now");
        } else {
            price.setText("Free");
            actionBtn.setText("Register for Free");
        }

        // ✅ UPDATED: Action Button Click
        actionBtn.setOnClickListener(v -> {
            if (amount > 0) {
                // 🚀 Launch Custom Payment Screen
                Intent intent = new Intent(this, PaymentMethodActivity.class);
                intent.putExtra("event_data", event);
                paymentLauncher.launch(intent);
            } else {
                // Free Event - Direct Register
                registerUser("FREE_TICKET");
            }
        });

        // View Students Click
        viewStudentsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisteredStudentsActivity.class);
            intent.putExtra("event_id", event.getEventId());
            startActivity(intent);
        });
    }

    private void checkUserRole() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        if ("leader".equals(role) || currentUid.equals(event.getCreatorId())) {
                            viewStudentsBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void checkIfAlreadyRegistered() {
        db.collection("tickets")
                .whereEqualTo("eventId", event.getEventId())
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        actionBtn.setText("✅ Already Registered");
                        actionBtn.setEnabled(false);
                        actionBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    }
                });
    }

    // ✅ Save Ticket to Firestore
    private void registerUser(String paymentId) {
        actionBtn.setText("Processing...");
        actionBtn.setEnabled(false);

        String ticketId = UUID.randomUUID().toString();
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("ticketId", ticketId);
        ticket.put("eventId", event.getEventId());
        ticket.put("eventTitle", event.getTitle());
        ticket.put("userId", currentUid);
        ticket.put("userName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        ticket.put("paymentId", paymentId);
        ticket.put("amount", event.getAmountInPaise());
        ticket.put("currency", "INR");
        ticket.put("verified", false);
        ticket.put("timestamp", System.currentTimeMillis());
        ticket.put("isCheckedIn", false);

        db.collection("tickets").document(ticketId)
                .set(ticket)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Confirmed!", Toast.LENGTH_LONG).show();
                    actionBtn.setText("✅ Registered");
                    actionBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    actionBtn.setEnabled(true);
                    actionBtn.setText("Register Now");
                });
    }
}
