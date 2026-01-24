package com.example.campus_sphere;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateEventFragment extends Fragment {

    private ImageView eventPosterPreview;
    private EditText titleInput, descInput, categoryInput, venueInput, priceInput, dateInput, timeInput;
    private Button generateCaptionBtn, uploadPosterBtn, publishEventBtn;
    private CheckBox enableAttendanceCheck;
    private Uri imageUri;
    private boolean isImageSelected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);


        try {
            Map config = new HashMap();
            config.put("cloud_name", "dpadbarxt");
            config.put("secure", true);
            MediaManager.init(getContext(), config);
        } catch (IllegalStateException e) {
            // Cloudinary is already initialized; safe to ignore.
        }


        // Bind Views
        eventPosterPreview = view.findViewById(R.id.eventPosterPreview);
        titleInput = view.findViewById(R.id.eventTitleInput);
        descInput = view.findViewById(R.id.eventDescInput);
        categoryInput = view.findViewById(R.id.eventCategoryInput);
        venueInput = view.findViewById(R.id.eventVenueInput);
        priceInput = view.findViewById(R.id.eventPriceInput);
        dateInput = view.findViewById(R.id.eventDateInput);
        timeInput = view.findViewById(R.id.eventTimeInput);

        generateCaptionBtn = view.findViewById(R.id.generateCaptionBtn);
        uploadPosterBtn = view.findViewById(R.id.uploadPosterBtn);
        publishEventBtn = view.findViewById(R.id.publishEventBtn);
        enableAttendanceCheck = view.findViewById(R.id.enableAttendanceCheck);

        // 1. DATE & TIME PICKERS
        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());

        // 2. IMAGE PICKER LOGIC
        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        isImageSelected = true;

                        // 🚀 FIX: Clear padding so image fills the card
                        eventPosterPreview.setPadding(0, 0, 0, 0);

                        // Load image using Glide
                        Glide.with(this).load(imageUri).centerCrop().into(eventPosterPreview);
                    }
                });

        uploadPosterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // 3. PUBLISH BUTTON
        publishEventBtn.setOnClickListener(v -> {
            if (!isImageSelected) {
                Toast.makeText(getContext(), "Please select a poster", Toast.LENGTH_SHORT).show();
                return;
            }
            if(titleInput.getText().toString().isEmpty()) {
                titleInput.setError("Required");
                return;
            }
            uploadImageToCloudinary();
        });

        // 4. AI CAPTION (Simulation)
        generateCaptionBtn.setOnClickListener(v -> {
            String title = titleInput.getText().toString();
            String cat = categoryInput.getText().toString();
            descInput.setText("🚀 Join us for " + title + "! An amazing " + cat + " event. Don't miss this opportunity at " + venueInput.getText().toString() + "! #CampusSphere");
        });

        return view;
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, day) ->
                dateInput.setText(day + "/" + (month + 1) + "/" + year),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(getContext(), (view, hour, minute) ->
                timeInput.setText(String.format("%02d:%02d", hour, minute)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private void uploadImageToCloudinary() {
        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();
        publishEventBtn.setEnabled(false);

        MediaManager.get().upload(imageUri)
                .unsigned("campus_sphere_preset") // 🔴 Ensure this matches your Cloudinary preset
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveEventToFirestore(imageUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo errorInfo) {
                        Toast.makeText(getContext(), "Upload Failed: " + errorInfo.getDescription(), Toast.LENGTH_SHORT).show();
                        publishEventBtn.setEnabled(true);
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                }).dispatch();
    }

    private void saveEventToFirestore(String imageUrl) {
        String eventId = UUID.randomUUID().toString();
        String price = priceInput.getText().toString().isEmpty() ? "Free" : "₹" + priceInput.getText().toString();

        Event newEvent = new Event(
                eventId,
                titleInput.getText().toString(),
                descInput.getText().toString(),
                categoryInput.getText().toString(),
                price,
                venueInput.getText().toString(),
                dateInput.getText().toString(),
                timeInput.getText().toString(),
                imageUrl,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                enableAttendanceCheck.isChecked()
        );

        FirebaseFirestore.getInstance().collection("events").document(eventId).set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Event Published!", Toast.LENGTH_LONG).show();

                    // Reset UI
                    titleInput.setText("");
                    descInput.setText("");
                    eventPosterPreview.setImageResource(android.R.drawable.ic_menu_camera);
                    eventPosterPreview.setPadding(60,60,60,60);

                    publishEventBtn.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    publishEventBtn.setEnabled(true);
                });
    }
}