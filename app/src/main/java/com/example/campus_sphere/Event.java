package com.example.campus_sphere;

import java.io.Serializable; // ✅ Required for Intent passing

public class Event implements Serializable { // ✅ Implements Serializable

    private String eventId;
    private String title;
    private String description;
    private String category;
    private String price;
    private String venue;
    private String date;
    private String time;
    private String imageUrl;
    private String creatorId;
    private boolean attendanceEnabled;

    public Event() {} // Required for Firestore

    public Event(String eventId, String title, String description, String category, String price, String venue, String date, String time, String imageUrl, String creatorId, boolean attendanceEnabled) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.venue = venue;
        this.date = date;
        this.time = time;
        this.imageUrl = imageUrl;
        this.creatorId = creatorId;
        this.attendanceEnabled = attendanceEnabled;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPrice() { return price; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getImageUrl() { return imageUrl; }
    public String getCreatorId() { return creatorId; }
    public boolean isAttendanceEnabled() { return attendanceEnabled; }

    // ✅ NEW HELPER: Converts "₹500" to 50000 (paise) for Razorpay
    public long getAmountInPaise() {
        if (price == null || price.toLowerCase().contains("free")) {
            return 0;
        }
        try {
            // Remove everything except numbers
            String cleanPrice = price.replaceAll("[^\\d]", "");
            if (cleanPrice.isEmpty()) return 0;
            return Long.parseLong(cleanPrice) * 100; // Convert Rupees to Paise
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}