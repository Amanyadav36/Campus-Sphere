package com.example.campus_sphere;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminEditClubActivity extends AppCompatActivity {

    public static final String EXTRA_CLUB_ID = "club_id";

    private EditText inputName;
    private EditText inputHandle;
    private EditText inputBio;
    private Button btnSave;
    private TextView tvStatus;

    private FirebaseFirestore db;
    private String clubId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_club);

        clubId = getIntent().getStringExtra(EXTRA_CLUB_ID);
        if (clubId == null || clubId.trim().isEmpty()) {
            Toast.makeText(this, "Club not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        inputName = findViewById(R.id.inputClubName);
        inputHandle = findViewById(R.id.inputClubHandle);
        inputBio = findViewById(R.id.inputClubBio);
        btnSave = findViewById(R.id.btnSaveClub);
        tvStatus = findViewById(R.id.tvEditClubStatus);

        btnSave.setOnClickListener(v -> save());
        load();
    }

    private void setStatus(String text) {
        if (tvStatus != null) tvStatus.setText(text != null ? text : "");
    }

    private void load() {
        setStatus("Loading...");
        db.collection("users").document(clubId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("clubName");
                    String handle = doc.getString("clubHandle");
                    String bio = doc.getString("clubBio");

                    if (name != null) inputName.setText(name);
                    if (handle != null) inputHandle.setText(handle);
                    if (bio != null) inputBio.setText(bio);
                    setStatus("");
                })
                .addOnFailureListener(e -> setStatus("Failed to load: " + e.getMessage()));
    }

    private void save() {
        String name = inputName.getText().toString().trim();
        String handle = inputHandle.getText().toString().trim();
        String bio = inputBio.getText().toString().trim();

        if (name.isEmpty()) {
            inputName.setError("Required");
            return;
        }

        btnSave.setEnabled(false);
        setStatus("Saving...");

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("clubName", name);
        userUpdates.put("clubHandle", handle);
        userUpdates.put("clubBio", bio);

        Map<String, Object> clubUpdates = new HashMap<>();
        clubUpdates.put("name", name);
        clubUpdates.put("handle", handle);
        clubUpdates.put("bio", bio);

        db.collection("users").document(clubId).update(userUpdates)
                .addOnSuccessListener(aVoid -> db.collection("clubs").document(clubId)
                        .set(clubUpdates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(v -> {
                            AdminAuditLogger.log("CLUB_EDIT", "club", clubId, "", name);
                            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            btnSave.setEnabled(true);
                            setStatus("Saved in users, but failed in clubs: " + e.getMessage());
                        }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    setStatus("Save failed: " + e.getMessage());
                });
    }
}

