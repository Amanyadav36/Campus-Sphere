package com.example.campus_sphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminPaymentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminPaymentAdapter adapter;
    private final List<PaymentItem> payments = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_admin_payments, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.adminPaymentsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminPaymentAdapter(payments, new AdminPaymentAdapter.OnPaymentActionListener() {
            @Override
            public void onVerify(PaymentItem item) {
                verifyPayment(item);
            }

            @Override
            public void onReject(PaymentItem item) {
                rejectPayment(item);
            }
        });
        recyclerView.setAdapter(adapter);

        fetchPayments();
        return view;
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
                        Toast.makeText(getContext(), "Failed to load payments", Toast.LENGTH_SHORT).show()
                );
    }

    private void verifyPayment(PaymentItem item) {
        if (item.getPaymentId() == null || "FREE_TICKET".equals(item.getPaymentId())) {
            Toast.makeText(getContext(), "Free ticket cannot be verified", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tickets").document(item.getId())
                .update("verified", true)
                .addOnSuccessListener(aVoid -> {
                    AdminAuditLogger.log("PAYMENT_VERIFY", "ticket", item.getId(), "unverified", "verified");
                    fetchPayments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Verify failed", Toast.LENGTH_SHORT).show()
                );
    }

    private void rejectPayment(PaymentItem item) {
        db.collection("tickets").document(item.getId())
                .update("verified", false)
                .addOnSuccessListener(aVoid -> {
                    AdminAuditLogger.log("PAYMENT_REJECT", "ticket", item.getId(), "unverified", "rejected");
                    fetchPayments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Reject failed", Toast.LENGTH_SHORT).show()
                );
    }
}
