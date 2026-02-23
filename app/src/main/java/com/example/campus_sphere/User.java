package com.example.campus_sphere;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String enrollment;
    private String branch;
    private String profileImage;
    private Boolean suspended;

    public User() {}

    public User(String uid, String name, String email, String role, String enrollment, String branch, String profileImage, Boolean suspended) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.enrollment = enrollment;
        this.branch = branch;
        this.profileImage = profileImage;
        this.suspended = suspended;
    }

    public User(String name, String branch, String enrollment, String profileImage) {
        this.name = name;
        this.branch = branch;
        this.enrollment = enrollment;
        this.profileImage = profileImage;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getEnrollment() { return enrollment; }
    public String getBranch() { return branch; }
    public String getProfileImage() { return profileImage; }
    public Boolean getSuspended() { return suspended; }

    public void setUid(String uid) { this.uid = uid; }
}
