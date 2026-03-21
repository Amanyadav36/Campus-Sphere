package com.example.campus_sphere;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AdminCreateEventActivity extends AppCompatActivity {

    private Spinner spinnerClub;
    private Spinner spinnerVenue;
    private EditText inputTitle;
    private EditText inputCategory;
    private EditText inputDate;
    private EditText inputTime;
    private EditText inputPrice;
    private EditText inputDescription;
    private Button btnCreate;
    private TextView tvStatus;

    private FirebaseFirestore db;

    private final List<IdName> clubs = new ArrayList<>();
    private ArrayAdapter<String> clubAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_event);

        db = FirebaseFirestore.getInstance();

        spinnerClub = findViewById(R.id.spinnerClub);
        spinnerVenue = findViewById(R.id.spinnerVenue);
        inputTitle = findViewById(R.id.inputTitle);
        inputCategory = findViewById(R.id.inputCategory);
        inputDate = findViewById(R.id.inputDate);
        inputTime = findViewById(R.id.inputTime);
        inputPrice = findViewById(R.id.inputPrice);
        inputDescription = findViewById(R.id.inputDescription);
        btnCreate = findViewById(R.id.btnCreateEvent);
        tvStatus = findViewById(R.id.tvCreateEventStatus);

        clubAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerClub.setAdapter(clubAdapter);

        String[] venues = {"Auditorium CDGI", "Auditorium CDIPS", "Seminar Hall 1", "Seminar Hall 2"};
        ArrayAdapter<String> venueAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, venues);
        spinnerVenue.setAdapter(venueAdapter);

        inputDate.setOnClickListener(v -> showDatePicker());
        inputTime.setOnClickListener(v -> showTimePicker());
        btnCreate.setOnClickListener(v -> createEvent());

        loadClubs();
    }

    private void setStatus(String text) {
        if (tvStatus != null) tvStatus.setText(text != null ? text : "");
    }

    private void loadClubs() {
        setStatus("Loading clubs...");
        clubs.clear();
        clubAdapter.clear();

        db.collection("clubs").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        if (name == null) name = doc.getString("clubName");
                        if (name == null) name = id;
                        clubs.add(new IdName(id, name));
                    }

                    if (clubs.isEmpty()) {
                        loadClubsFromUsersFallback();
                        return;
                    }

                    for (IdName c : clubs) clubAdapter.add(c.name);
                    clubAdapter.notifyDataSetChanged();
                    setStatus("");
                })
                .addOnFailureListener(e -> loadClubsFromUsersFallback());
    }

    private void loadClubsFromUsersFallback() {
        db.collection("users").whereEqualTo("role", "leader").get()
                .addOnSuccessListener(snap -> {
                    clubs.clear();
                    clubAdapter.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("clubName");
                        if (name == null) name = doc.getString("name");
                        if (name == null) name = id;
                        clubs.add(new IdName(id, name));
                    }
                    for (IdName c : clubs) clubAdapter.add(c.name);
                    clubAdapter.notifyDataSetChanged();
                    setStatus("");
                })
                .addOnFailureListener(e -> setStatus("Failed to load clubs: " + e.getMessage()));
    }

    private void createEvent() {
        int clubPos = spinnerClub.getSelectedItemPosition();
        if (clubPos < 0 || clubPos >= clubs.size()) {
            Toast.makeText(this, "Select a club", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = inputTitle.getText().toString().trim();
        String category = inputCategory.getText().toString().trim();
        String date = inputDate.getText().toString().trim();
        String time = inputTime.getText().toString().trim();
        String venue = spinnerVenue.getSelectedItem() != null ? spinnerVenue.getSelectedItem().toString() : "";
        String desc = inputDescription.getText().toString().trim();
        String priceRaw = inputPrice.getText().toString().trim();

        if (title.isEmpty()) {
            inputTitle.setError("Required");
            return;
        }
        if (date.isEmpty()) {
            inputDate.setError("Required");
            return;
        }
        if (time.isEmpty()) {
            inputTime.setError("Required");
            return;
        }

        String clubId = clubs.get(clubPos).id;
        String eventId = UUID.randomUUID().toString();

        String price = priceRaw.isEmpty() ? "Free" : ("Rs" + priceRaw);

        Event event = new Event(
                eventId,
                title,
                desc,
                category,
                price,
                venue,
                date,
                time,
                "",
                clubId, // creatorId
                false
        );
        event.setClubId(clubId);

        btnCreate.setEnabled(false);
        setStatus("Checking availability...");

        // Pre-check against existing events (covers legacy events that may not have slot docs yet).
        db.collection("events")
                .whereEqualTo("venue", venue)
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    if (snap != null && !snap.isEmpty()) {
                        btnCreate.setEnabled(true);
                        setStatus("");
                        Toast.makeText(this, venue + " is already booked for that slot.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    setStatus("Creating event...");
                    VenueSlotManager.createEventWithLock(db, event)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Event created", Toast.LENGTH_SHORT).show();
                                setStatus("Created.");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnCreate.setEnabled(true);
                                String msg = e.getMessage() != null && e.getMessage().contains("VENUE_SLOT_TAKEN")
                                        ? (venue + " is already booked for that slot.")
                                        : ("Failed: " + e.getMessage());
                                setStatus(msg);
                            });
                })
                .addOnFailureListener(e -> {
                    // If check fails, still try locking transaction to prevent double booking.
                    setStatus("Creating event...");
                    VenueSlotManager.createEventWithLock(db, event)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Event created", Toast.LENGTH_SHORT).show();
                                setStatus("Created.");
                                finish();
                            })
                            .addOnFailureListener(ex -> {
                                btnCreate.setEnabled(true);
                                setStatus("Failed: " + ex.getMessage());
                            });
                });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String text = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            inputDate.setText(text);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            inputTime.setText(text);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private static final class IdName {
        final String id;
        final String name;

        IdName(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
