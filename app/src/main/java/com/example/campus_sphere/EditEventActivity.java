package com.example.campus_sphere;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private ImageView eventPosterPreview;
    private EditText titleInput, descInput, categoryInput, priceInput, dateInput, timeInput;
    private Spinner venueSpinner;
    private Button uploadPosterBtn, publishEventBtn;
    private CheckBox enableAttendanceCheck;
    private TextView formTitle;

    private Uri imageUri;
    private boolean isImageSelected = false;
    private Event event;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_event);

        db = FirebaseFirestore.getInstance();
        initCloudinarySafe();

        event = (Event) getIntent().getSerializableExtra("event_data");
        if (event == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventPosterPreview = findViewById(R.id.eventPosterPreview);
        titleInput = findViewById(R.id.eventTitleInput);
        descInput = findViewById(R.id.eventDescInput);
        categoryInput = findViewById(R.id.eventCategoryInput);
        venueSpinner = findViewById(R.id.eventVenueSpinner);
        priceInput = findViewById(R.id.eventPriceInput);
        dateInput = findViewById(R.id.eventDateInput);
        timeInput = findViewById(R.id.eventTimeInput);
        uploadPosterBtn = findViewById(R.id.uploadPosterBtn);
        publishEventBtn = findViewById(R.id.publishEventBtn);
        enableAttendanceCheck = findViewById(R.id.enableAttendanceCheck);
        formTitle = findViewById(R.id.eventFormTitle);

        Button generateCaptionBtn = findViewById(R.id.generateCaptionBtn);
        generateCaptionBtn.setVisibility(View.GONE);

        formTitle.setText("Edit Event");
        publishEventBtn.setText("Update Event");

        String[] venues = {"Auditorium CDGI", "Auditorium CDIPS", "Seminar Hall 1", "Seminar Hall 2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, venues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        venueSpinner.setAdapter(adapter);

        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            eventPosterPreview.setPadding(0, 0, 0, 0);
            Glide.with(this).load(event.getImageUrl()).centerCrop().into(eventPosterPreview);
        }

        titleInput.setText(event.getTitle());
        descInput.setText(event.getDescription());
        categoryInput.setText(event.getCategory());
        dateInput.setText(event.getDate());
        timeInput.setText(event.getTime());
        enableAttendanceCheck.setChecked(event.isAttendanceEnabled());

        String priceValue = event.getPrice();
        if (priceValue != null && !priceValue.toLowerCase().contains("free")) {
            priceInput.setText(priceValue.replaceAll("[^\\d]", ""));
        }

        if (event.getVenue() != null) {
            int position = adapter.getPosition(event.getVenue());
            if (position >= 0) {
                venueSpinner.setSelection(position);
            }
        }

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        isImageSelected = true;
                        eventPosterPreview.setPadding(0, 0, 0, 0);
                        Glide.with(this).load(imageUri).centerCrop().into(eventPosterPreview);
                    }
                });

        uploadPosterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        publishEventBtn.setOnClickListener(v -> {
            if (!validateInputs()) return;
            if (isImageSelected) {
                uploadImageToCloudinary();
            } else {
                updateEventInFirestore(event.getImageUrl());
            }
        });
    }

    private void initCloudinarySafe() {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", "dpadbarxt");
                config.put("secure", true);
                MediaManager.init(getApplicationContext(), config);
            } catch (Exception ex) {
                Log.e("Cloudinary", "Init failed", ex);
            }
        }
    }

    private boolean validateInputs() {
        if (titleInput.getText().toString().trim().isEmpty()) {
            titleInput.setError("Required");
            return false;
        }
        if (dateInput.getText().toString().trim().isEmpty()
                || timeInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Date and Time are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImageToCloudinary() {
        publishEventBtn.setEnabled(false);
        publishEventBtn.setText("Uploading Poster...");

        MediaManager.get().upload(imageUri)
                .unsigned("campus_sphere_preset")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        updateEventInFirestore(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo errorInfo) {
                        Toast.makeText(EditEventActivity.this,
                                "Upload Failed: " + errorInfo.getDescription(),
                                Toast.LENGTH_SHORT).show();
                        publishEventBtn.setEnabled(true);
                        publishEventBtn.setText("Update Event");
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                }).dispatch();
    }

    private void updateEventInFirestore(String imageUrl) {
        String price = priceInput.getText().toString().trim();
        String priceValue = price.isEmpty() ? "Free" : "₹" + price;

        String newVenue = venueSpinner.getSelectedItem().toString();
        String newDate = dateInput.getText().toString().trim();
        String newTime = timeInput.getText().toString().trim();

        // Old slot (best effort for legacy events).
        final String oldSlotIdFinal = (event.getVenueSlotId() != null && !event.getVenueSlotId().trim().isEmpty())
                ? event.getVenueSlotId()
                : VenueSlotManager.buildSlotId(event.getVenue(), event.getDate(), event.getTime());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", titleInput.getText().toString().trim());
        updates.put("description", descInput.getText().toString().trim());
        updates.put("category", categoryInput.getText().toString().trim());
        updates.put("price", priceValue);
        updates.put("venue", newVenue);
        updates.put("date", newDate);
        updates.put("time", newTime);
        updates.put("imageUrl", imageUrl);
        updates.put("attendanceEnabled", enableAttendanceCheck.isChecked());
        updates.put("creatorId", FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : event.getCreatorId());

        publishEventBtn.setEnabled(false);
        publishEventBtn.setText("Checking Availability...");

        // Pre-check: find any other event in same slot (venue+date+time).
        db.collection("events")
                .whereEqualTo("venue", newVenue)
                .whereEqualTo("date", newDate)
                .whereEqualTo("time", newTime)
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    boolean conflict = false;
                    if (snap != null && !snap.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            if (doc.getId().equals(event.getEventId())) continue;
                            String eid = doc.getString("eventId");
                            if (eid != null && eid.equals(event.getEventId())) continue;
                            conflict = true;
                            break;
                        }
                    }
                    if (conflict) {
                        Toast.makeText(this, "⚠️ " + newVenue + " is already booked for that time.", Toast.LENGTH_LONG).show();
                        publishEventBtn.setEnabled(true);
                        publishEventBtn.setText("Update Event");
                        return;
                    }

                    publishEventBtn.setText("Updating...");
                    VenueSlotManager.updateEventWithLock(db, event.getEventId(), oldSlotIdFinal, newVenue, newDate, newTime, updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                String msg = e.getMessage() != null && e.getMessage().contains("VENUE_SLOT_TAKEN")
                                        ? ("⚠️ " + newVenue + " is already booked for that time.")
                                        : ("Update failed: " + e.getMessage());
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                                publishEventBtn.setEnabled(true);
                                publishEventBtn.setText("Update Event");
                            });
                })
                .addOnFailureListener(e -> {
                    // If check fails, still enforce lock transaction to prevent double booking.
                    publishEventBtn.setText("Updating...");
                    VenueSlotManager.updateEventWithLock(db, event.getEventId(), oldSlotIdFinal, newVenue, newDate, newTime, updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(ex -> {
                                Toast.makeText(this, "Update failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                publishEventBtn.setEnabled(true);
                                publishEventBtn.setText("Update Event");
                            });
                });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                dateInput.setText(day + "/" + (month + 1) + "/" + year),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) ->
                timeInput.setText(String.format("%02d:%02d", hour, minute)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }
}
