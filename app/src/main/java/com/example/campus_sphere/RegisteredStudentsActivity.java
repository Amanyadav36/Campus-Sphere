package com.example.campus_sphere;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class RegisteredStudentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<User> studentList;
    private TextView emptyStateText;
    private FirebaseFirestore db;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_students);

        eventId = getIntent().getStringExtra("event_id");
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.studentsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        studentList = new ArrayList<>();
        adapter = new StudentAdapter(studentList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (eventId != null) {
            loadRegisteredStudents();
        }
    }

    private void loadRegisteredStudents() {
        // 1. First, find all tickets for this event
        db.collection("tickets")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        emptyStateText.setVisibility(View.VISIBLE);
                        return;
                    }

                    emptyStateText.setVisibility(View.GONE);

                    // 2. Get the User ID from each ticket
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("userId");
                        if (userId != null) {
                            fetchUserDetails(userId);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchUserDetails(String userId) {
        // 3. Fetch full details (Name, Branch, RollNo) from 'users' collection
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        studentList.add(user);
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}