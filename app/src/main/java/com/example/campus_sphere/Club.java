package com.example.campus_sphere;

public class Club {
    private final String id;
    private final String name;
    private final String handle;
    private final String bio;
    private final String logoUrl;
    private final String headerUrl;
    private boolean isJoined;

    public Club(String id, String name, String handle, String bio, String logoUrl, String headerUrl) {
        this.id = id;
        this.name = name;
        this.handle = handle;
        this.bio = bio;
        this.logoUrl = logoUrl;
        this.headerUrl = headerUrl;
        this.isJoined = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getHandle() { return handle; }
    public String getBio() { return bio; }
    public String getLogoUrl() { return logoUrl; }
    public String getHeaderUrl() { return headerUrl; }
    public boolean isJoined() { return isJoined; }
    public void setJoined(boolean joined) { isJoined = joined; }
}
