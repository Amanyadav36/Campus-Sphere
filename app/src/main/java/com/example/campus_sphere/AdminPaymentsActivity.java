package com.example.campus_sphere;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminPaymentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminPaymentAdapter adapter;
    private final List<PaymentItem> payments = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_payments);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.adminPaymentsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminPaymentAdapter(payments, new AdminPaymentAdapter.OnPaymentActionListener() {
            @Override
            public void onVerify(PaymentItem item) {
                // No-op in activity mode
            }

            @Override
            public void onReject(PaymentItem item) {
                // No-op in activity mode
            }
        });
        recyclerView.setAdapter(adapter);

        fetchPayments();
    }

    private void fetchPayments() {
        db.collection("tickets").get()
                .addOnSuccessListener(snapshot -> {
                    payments.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String eventId = doc.getString("eventId");
                        String eventTitle = doc.getString("eventTitle");
                        String userName = doc.getString("userName");
                        String paymentId = doc.getString("paymentId");
                        Boolean verified = doc.getBoolean("verified");
                        payments.add(new PaymentItem(doc.getId(), eventId, eventTitle, userName, paymentId, verified));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load payments", Toast.LENGTH_SHORT).show()
                );
    }
}
