package com.example.campus_sphere;

public class AdminEventItem {
    private final Event event;
    private final boolean featured;

    public AdminEventItem(Event event, boolean featured) {
        this.event = event;
        this.featured = featured;
    }

    public Event getEvent() { return event; }
    public boolean isFeatured() { return featured; }
}
