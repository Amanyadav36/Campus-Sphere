package com.example.campus_sphere;

public class PaymentItem {
    private final String id;
    private final String eventId;
    private final String eventTitle;
    private final String userName;
    private final String paymentId;
    private final Boolean verified;

    public PaymentItem(String id, String eventId, String eventTitle, String userName, String paymentId, Boolean verified) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.userName = userName;
        this.paymentId = paymentId;
        this.verified = verified;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getUserName() { return userName; }
    public String getPaymentId() { return paymentId; }
    public Boolean getVerified() { return verified; }
}
