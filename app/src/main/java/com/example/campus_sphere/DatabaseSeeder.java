package com.example.campus_sphere;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DatabaseSeeder {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String SEED_KEY = "isDataSeeded";

    public static void seedData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(SEED_KEY, false)) {
            Log.d("DatabaseSeeder", "Data already seeded. Skipping.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Create 4 Clubs (as "users" with role "leader")
        createClub(db, "club_1", "Tech Innovators Club", "tech_innovators", "Pioneering the future of technology on campus.", "https://images.unsplash.com/photo-1518770660439-4636190af475?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80");
        createClub(db, "club_2", "Arts & Culture Society", "arts_culture", "Celebrating diversity through art, music, and performance.", "https://images.unsplash.com/photo-1513364776144-60967b0f800f?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80", "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80");
        createClub(db, "club_3", "Sports & Athletics", "campus_sports", "Promoting health, fitness, and competitive sportsmanship.", "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80", "https://images.unsplash.com/photo-1483726234545-481d6e8804cb?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80");
        createClub(db, "club_4", "Coding Ninjas", "coding_ninjas", "Master algorithms, participate in hackathons, and code together.", "https://images.unsplash.com/photo-1542831371-29b0f74f9713?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80");

        // 2. Create 5 Events
        createEvent(db, "event_1", "club_1", "Tech Symposium 2026", "Join us for a 3-day tech extravaganza.", "Technology", "Free", "Auditorium 1", "2026-03-10", "10:00 AM", "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80");
        createEvent(db, "event_2", "club_2", "Annual Art Exhibition", "Showcasing student talents across painting and digital arts.", "Arts", "₹ 200", "Main Gallery", "2026-03-15", "04:00 PM", "https://images.unsplash.com/photo-1513364776144-60967b0f800f?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80");
        createEvent(db, "event_3", "club_3", "Inter-Department Football Match", "Support your department in the grand finale!", "Sports", "Free", "Campus Ground", "2026-03-20", "05:00 PM", "https://images.unsplash.com/photo-1518605368461-1ee01509b558?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80");
        createEvent(db, "event_4", "club_4", "24-Hour Hackathon", "Code, collaborate, and win exciting prizes!", "Technology", "₹ 500", "CS Block Lab", "2026-03-25", "09:00 AM", "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80");
        createEvent(db, "event_5", "club_2", "Acoustic Night", "Relax and enjoy live acoustic music performances by students.", "Music", "₹ 150", "Student Lounge", "2026-04-05", "07:30 PM", "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?ixlib=rb-1.2.1&auto=format&fit=crop&w=400&q=80");

        prefs.edit().putBoolean(SEED_KEY, true).apply();
        Log.d("DatabaseSeeder", "Data successfully seeded.");
    }

    private static void createClub(FirebaseFirestore db, String uid, String name, String handle, String bio, String logo, String header) {
        Map<String, Object> club = new HashMap<>();
        club.put("name", name);
        club.put("role", "leader");
        club.put("clubName", name);
        club.put("clubHandle", handle);
        club.put("clubBio", bio);
        club.put("clubLogo", logo);
        club.put("headerImage", header);

        db.collection("users").document(uid).set(club)
                .addOnFailureListener(e -> Log.e("DatabaseSeeder", "Failed to insert club: " + e.getMessage()));
    }

    private static void createEvent(FirebaseFirestore db, String eventId, String creatorId, String title, String description, String category, String price, String venue, String date, String time, String imageUrl) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("creatorId", creatorId);
        event.put("title", title);
        event.put("description", description);
        event.put("category", category);
        event.put("price", price);
        event.put("venue", venue);
        event.put("date", date);
        event.put("time", time);
        event.put("imageUrl", imageUrl);
        event.put("attendanceEnabled", false);
        event.put("timestamp", System.currentTimeMillis());

        db.collection("events").document(eventId).set(event)
                .addOnFailureListener(e -> Log.e("DatabaseSeeder", "Failed to insert event: " + e.getMessage()));
    }
}
