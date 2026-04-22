package com.example.campus_sphere;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.google.firebase.firestore.QuerySnapshot;

// Gemini Imports
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CreateEventFragment extends Fragment {

    // Removed hardcoded key
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    private ImageView eventPosterPreview;
    private EditText titleInput, descInput, categoryInput, priceInput, dateInput, timeInput;
    private Spinner venueSpinner;
    private Button generateCaptionBtn, uploadPosterBtn, publishEventBtn;
    private CheckBox enableAttendanceCheck;
    private Uri imageUri;
    private boolean isImageSelected = false;

    private GenerativeModelFutures model;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        db = FirebaseFirestore.getInstance();

        // 1. SAFE CLOUDINARY INIT (Prevents Crash)
        initCloudinarySafe();

        // 2. Gemini Init
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY);
        model = GenerativeModelFutures.from(gm);

        // Bind Views
        eventPosterPreview = view.findViewById(R.id.eventPosterPreview);
        titleInput = view.findViewById(R.id.eventTitleInput);
        descInput = view.findViewById(R.id.eventDescInput);
        categoryInput = view.findViewById(R.id.eventCategoryInput);
        venueSpinner = view.findViewById(R.id.eventVenueSpinner);
        priceInput = view.findViewById(R.id.eventPriceInput);
        dateInput = view.findViewById(R.id.eventDateInput);
        timeInput = view.findViewById(R.id.eventTimeInput);

        generateCaptionBtn = view.findViewById(R.id.generateCaptionBtn);
        uploadPosterBtn = view.findViewById(R.id.uploadPosterBtn);
        publishEventBtn = view.findViewById(R.id.publishEventBtn);
        enableAttendanceCheck = view.findViewById(R.id.enableAttendanceCheck);

        // ✅ SETUP VENUE SPINNER (With Black Text)
        String[] venues = {"Auditorium CDGI", "Auditorium CDIPS", "Seminar Hall 1", "Seminar Hall 2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, venues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        venueSpinner.setAdapter(adapter);

        // Date & Time Pickers
        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());

        // Image Picker
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

        // Publish Logic
        publishEventBtn.setOnClickListener(v -> {
            if (!validateInputs()) return;
            checkVenueConflictAndPublish();
        });

        generateCaptionBtn.setOnClickListener(v -> generateCreativeCaption());

        return view;
    }

    // ✅ SAFE INITIALIZATION METHOD
    private void initCloudinarySafe() {
        try {
            // Check if already initialized
            MediaManager.get();
        } catch (IllegalStateException e) {
            // If not, initialize it now
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
                config.put("secure", true);
                MediaManager.init(requireContext(), config);
            } catch (Exception ex) {
                Log.e("Cloudinary", "Init failed", ex);
            }
        }
    }

    private boolean validateInputs() {
        if (!isImageSelected) {
            Toast.makeText(getContext(), "Please select a poster", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (titleInput.getText().toString().isEmpty()) {
            titleInput.setError("Required");
            return false;
        }
        if (dateInput.getText().toString().isEmpty() || timeInput.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Date and Time are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ✅ VENUE CONFLICT CHECK
    private void checkVenueConflictAndPublish() {
        publishEventBtn.setEnabled(false);
        publishEventBtn.setText("Checking Availability...");

        String selectedVenue = venueSpinner.getSelectedItem().toString();
        String selectedDate = dateInput.getText().toString();
        String selectedTime = timeInput.getText().toString();

        db.collection("events")
                .whereEqualTo("venue", selectedVenue)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("time", selectedTime)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            Toast.makeText(getContext(), "⚠️ " + selectedVenue + " is already booked!", Toast.LENGTH_LONG).show();
                            publishEventBtn.setEnabled(true);
                            publishEventBtn.setText("Publish Event");
                        } else {
                            uploadImageToCloudinary();
                        }
                    } else {
                        uploadImageToCloudinary(); // Fallback if check fails
                    }
                });
    }

    private void uploadImageToCloudinary() {
        publishEventBtn.setText("Uploading Poster...");

        MediaManager.get().upload(imageUri)
                .unsigned("campus_sphere_preset")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveEventToFirestore(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo errorInfo) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Upload Failed: " + errorInfo.getDescription(), Toast.LENGTH_SHORT).show();
                            publishEventBtn.setEnabled(true);
                            publishEventBtn.setText("Publish Event");
                        }
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                }).dispatch();
    }

    private void saveEventToFirestore(String imageUrl) {
        String eventId = UUID.randomUUID().toString();
        String price = priceInput.getText().toString().isEmpty() ? "Free" : "₹" + priceInput.getText().toString();
        String selectedVenue = venueSpinner.getSelectedItem().toString();

        Event newEvent = new Event(
                eventId,
                titleInput.getText().toString(),
                descInput.getText().toString(),
                categoryInput.getText().toString(),
                price,
                selectedVenue,
                dateInput.getText().toString(),
                timeInput.getText().toString(),
                imageUrl,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                enableAttendanceCheck.isChecked()
        );

        // Enforce locking at write-time (prevents race conditions).
        VenueSlotManager.createEventWithLock(db, newEvent)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "✅ Event Published!", Toast.LENGTH_LONG).show();
                        resetUI();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        String msg = "DB Error: " + e.getMessage();
                        if (e.getMessage() != null && e.getMessage().contains("VENUE_SLOT_TAKEN")) {
                            msg = "⚠️ " + selectedVenue + " is already booked for that time.";
                        }
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        publishEventBtn.setEnabled(true);
                        publishEventBtn.setText("Publish Event");
                    }
                });
    }

    private void resetUI() {
        titleInput.setText("");
        descInput.setText("");
        categoryInput.setText("");
        priceInput.setText("");
        dateInput.setText("");
        timeInput.setText("");
        eventPosterPreview.setImageResource(android.R.drawable.ic_menu_camera);
        eventPosterPreview.setPadding(60, 60, 60, 60);
        isImageSelected = false;
        publishEventBtn.setEnabled(true);
        publishEventBtn.setText("Publish Event");
    }

    private void generateCreativeCaption() {
        String title = titleInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String venue = venueSpinner.getSelectedItem().toString();

        if (title.isEmpty() || category.isEmpty()) {
            Toast.makeText(getContext(), "Fill Title & Category first!", Toast.LENGTH_SHORT).show();
            return;
        }

        generateCaptionBtn.setEnabled(false);
        generateCaptionBtn.setText("Generating...");

        String prompt = "Write a hype Instagram caption for a college event.\nTitle: " + title + "\nTheme: " + category + "\nVenue: " + venue + "\nKeep it under 100 words with emojis.";
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        descInput.setText(result.getText());
                        generateCaptionBtn.setText("✨ Regenerate");
                        generateCaptionBtn.setEnabled(true);
                    });
                }
            }
            @Override
            public void onFailure(Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "AI Error", Toast.LENGTH_SHORT).show();
                        generateCaptionBtn.setEnabled(true);
                    });
                }
            }
        }, executor);
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
}
