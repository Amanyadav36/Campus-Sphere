package com.example.campus_sphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeFragment extends Fragment {

    private TextView tvUsers;
    private TextView tvClubs;
    private TextView tvEvents;
    private TextView tvRegistrations;
    private TextView tvStatus;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        db = FirebaseFirestore.getInstance();

        tvUsers = view.findViewById(R.id.tvAdminUsers);
        tvClubs = view.findViewById(R.id.tvAdminClubs);
        tvEvents = view.findViewById(R.id.tvAdminEvents);
        tvRegistrations = view.findViewById(R.id.tvAdminRegistrations);
        tvStatus = view.findViewById(R.id.tvAdminHomeStatus);

        loadStats();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void setStatus(String text) {
        if (tvStatus != null) tvStatus.setText(text != null ? text : "");
    }

    private void loadStats() {
        setStatus("Loading...");
        loadCount("users", tvUsers);
        loadClubsCount();
        loadCount("events", tvEvents);
        loadCount("tickets", tvRegistrations);
    }

    private void loadClubsCount() {
        if (tvClubs == null) return;
        tvClubs.setText("0");

        db.collection("clubs")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    long count = snap.getCount();
                    if (count > 0) {
                        tvClubs.setText(String.valueOf(count));
                        setStatus("");
                        return;
                    }
                    // Legacy fallback: clubs stored on leader user docs.
                    db.collection("users").whereEqualTo("role", "leader").get()
                            .addOnSuccessListener(s -> {
                                if (!isAdded()) return;
                                tvClubs.setText(String.valueOf(s.size()));
                                setStatus("");
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                tvClubs.setText("0");
                                setStatus("");
                            });
                })
                .addOnFailureListener(e -> {
                    db.collection("users").whereEqualTo("role", "leader").get()
                            .addOnSuccessListener(s -> {
                                if (!isAdded()) return;
                                tvClubs.setText(String.valueOf(s.size()));
                                setStatus("");
                            })
                            .addOnFailureListener(ex -> {
                                if (!isAdded()) return;
                                tvClubs.setText("-");
                                setStatus("Some counts failed to load.");
                            });
                });
    }

    private void loadCount(String collection, TextView target) {
        if (target == null) return;
        target.setText("0");

        db.collection(collection)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener((AggregateQuerySnapshot snap) -> {
                    if (!isAdded()) return;
                    target.setText(String.valueOf(snap.getCount()));
                    setStatus("");
                })
                .addOnFailureListener(e -> {
                    // Fallback for older SDK behavior or security rules: do a full get.
                    db.collection(collection).get()
                            .addOnSuccessListener(s -> {
                                if (!isAdded()) return;
                                target.setText(String.valueOf(s.size()));
                                setStatus("");
                            })
                            .addOnFailureListener(ex -> {
                                if (!isAdded()) return;
                                target.setText("-");
                                setStatus("Some counts failed to load.");
                            });
                });
    }
}
