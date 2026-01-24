package com.example.campus_sphere;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private String category;    // New
    private String price;       // New (e.g., "Free" or "₹500")
    private String venue;       // New
    private String date;        // New
    private String time;        // New
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
}