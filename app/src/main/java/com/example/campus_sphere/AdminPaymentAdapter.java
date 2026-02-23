package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminPaymentAdapter extends RecyclerView.Adapter<AdminPaymentAdapter.PaymentViewHolder> {

    public interface OnPaymentActionListener {
        void onVerify(PaymentItem item);
        void onReject(PaymentItem item);
    }

    private final List<PaymentItem> payments;
    private final OnPaymentActionListener listener;

    public AdminPaymentAdapter(List<PaymentItem> payments, OnPaymentActionListener listener) {
        this.payments = payments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        PaymentItem item = payments.get(position);
        holder.eventTitle.setText(item.getEventTitle() != null ? item.getEventTitle() : "Event");
        holder.userName.setText(item.getUserName() != null ? item.getUserName() : "User");
        holder.paymentId.setText(item.getPaymentId() != null ? item.getPaymentId() : "FREE_TICKET");

        boolean paid = item.getPaymentId() != null && !item.getPaymentId().equals("FREE_TICKET");
        boolean verified = item.getVerified() != null && item.getVerified();
        holder.status.setText(verified ? "Verified" : (paid ? "Unverified" : "Free"));
        holder.status.setTextColor(verified ? 0xFF00B894 : (paid ? 0xFFE17055 : 0xFF636E72));

        holder.verifyBtn.setEnabled(paid && !verified);
        holder.rejectBtn.setEnabled(paid && !verified);
        holder.verifyBtn.setOnClickListener(v -> {
            if (listener != null) listener.onVerify(item);
        });
        holder.rejectBtn.setOnClickListener(v -> {
            if (listener != null) listener.onReject(item);
        });
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle;
        TextView userName;
        TextView paymentId;
        TextView status;
        Button verifyBtn;
        Button rejectBtn;

        PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.adminPaymentEvent);
            userName = itemView.findViewById(R.id.adminPaymentUser);
            paymentId = itemView.findViewById(R.id.adminPaymentId);
            status = itemView.findViewById(R.id.adminPaymentStatus);
            verifyBtn = itemView.findViewById(R.id.adminPaymentVerifyBtn);
            rejectBtn = itemView.findViewById(R.id.adminPaymentRejectBtn);
        }
    }
}
