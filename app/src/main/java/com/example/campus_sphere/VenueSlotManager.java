package com.example.campus_sphere;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Venue locking using Firestore transactions.
 *
 * Creates a unique "venue slot" document per (venue, date, time). This prevents two events
 * from being booked at the same venue + date + time, even under concurrency.
 *
 * Collection: venue_slots/{slotId}
 * slotId = normalize(venue) + "__" + normalize(date) + "__" + normalize(time)
 */
public final class VenueSlotManager {

    private static final String COL_SLOTS = "venue_slots";

    private VenueSlotManager() {}

    public static String buildSlotId(String venue, String date, String time) {
        return normalizeVenue(venue) + "__" + normalizeDate(date) + "__" + normalizeTime(time);
    }

    public static Task<Void> createEventWithLock(FirebaseFirestore db, Event event) {
        if (db == null) throw new IllegalArgumentException("db is null");
        if (event == null) throw new IllegalArgumentException("event is null");
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("eventId is missing");
        }

        String slotId = buildSlotId(event.getVenue(), event.getDate(), event.getTime());
        event.setVenueSlotId(slotId);

        DocumentReference eventRef = db.collection("events").document(event.getEventId());
        DocumentReference slotRef = db.collection(COL_SLOTS).document(slotId);

        Map<String, Object> slot = new HashMap<>();
        slot.put("slotId", slotId);
        slot.put("eventId", event.getEventId());
        slot.put("creatorId", event.getCreatorId());
        slot.put("clubId", event.getClubId());
        slot.put("venue", event.getVenue());
        slot.put("date", event.getDate());
        slot.put("time", event.getTime());
        slot.put("createdAt", FieldValue.serverTimestamp());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot slotSnap = transaction.get(slotRef);
            if (slotSnap.exists()) {
                throw new RuntimeException("VENUE_SLOT_TAKEN");
            }
            transaction.set(slotRef, slot);
            transaction.set(eventRef, event);
            return null;
        });
    }

    public static Task<Void> updateEventWithLock(FirebaseFirestore db, String eventId, String oldSlotId, String newVenue, String newDate, String newTime, Map<String, Object> eventUpdates) {
        if (db == null) throw new IllegalArgumentException("db is null");
        if (eventId == null || eventId.trim().isEmpty()) throw new IllegalArgumentException("eventId missing");
        if (eventUpdates == null) throw new IllegalArgumentException("eventUpdates is null");

        String newSlotId = buildSlotId(newVenue, newDate, newTime);
        eventUpdates.put("venueSlotId", newSlotId);

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference newSlotRef = db.collection(COL_SLOTS).document(newSlotId);
        DocumentReference oldSlotRef = (oldSlotId != null && !oldSlotId.trim().isEmpty())
                ? db.collection(COL_SLOTS).document(oldSlotId)
                : null;

        Map<String, Object> slot = new HashMap<>();
        slot.put("slotId", newSlotId);
        slot.put("eventId", eventId);
        Object creatorId = eventUpdates.get("creatorId");
        if (creatorId instanceof String) slot.put("creatorId", creatorId);
        Object clubId = eventUpdates.get("clubId");
        if (clubId instanceof String) slot.put("clubId", clubId);
        slot.put("venue", newVenue);
        slot.put("date", newDate);
        slot.put("time", newTime);
        slot.put("updatedAt", FieldValue.serverTimestamp());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            // If the new slot is different, ensure it is free.
            DocumentSnapshot newSnap = transaction.get(newSlotRef);
            if (newSnap.exists()) {
                String existingEventId = newSnap.getString("eventId");
                if (existingEventId == null || !existingEventId.equals(eventId)) {
                    throw new RuntimeException("VENUE_SLOT_TAKEN");
                }
            }

            // Release old slot if it belongs to this event.
            if (oldSlotRef != null) {
                DocumentSnapshot oldSnap = transaction.get(oldSlotRef);
                if (oldSnap.exists()) {
                    String existingEventId = oldSnap.getString("eventId");
                    if (eventId.equals(existingEventId) && !oldSlotId.equals(newSlotId)) {
                        transaction.delete(oldSlotRef);
                    }
                }
            }

            // Ensure slot doc exists for this event.
            transaction.set(newSlotRef, slot, com.google.firebase.firestore.SetOptions.merge());
            transaction.update(eventRef, eventUpdates);
            return null;
        });
    }

    public static Task<Void> deleteEventAndReleaseLock(FirebaseFirestore db, String eventId) {
        if (db == null) throw new IllegalArgumentException("db is null");
        if (eventId == null || eventId.trim().isEmpty()) throw new IllegalArgumentException("eventId missing");

        DocumentReference eventRef = db.collection("events").document(eventId);

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) return null;

            String slotId = eventSnap.getString("venueSlotId");
            if (slotId == null || slotId.trim().isEmpty()) {
                String venue = eventSnap.getString("venue");
                String date = eventSnap.getString("date");
                String time = eventSnap.getString("time");
                slotId = buildSlotId(venue, date, time);
            }

            DocumentReference slotRef = db.collection(COL_SLOTS).document(slotId);
            DocumentSnapshot slotSnap = transaction.get(slotRef);
            if (slotSnap.exists()) {
                String slotEventId = slotSnap.getString("eventId");
                if (eventId.equals(slotEventId)) {
                    transaction.delete(slotRef);
                }
            }

            transaction.delete(eventRef);
            return null;
        });
    }

    private static String normalizeVenue(String venue) {
        String v = venue != null ? venue.trim().toLowerCase(Locale.getDefault()) : "";
        if (v.isEmpty()) v = "unknown_venue";
        v = v.replaceAll("\\s+", "_");
        v = v.replaceAll("[^a-z0-9_]", "");
        if (v.isEmpty()) v = "unknown_venue";
        return v;
    }

    private static String normalizeDate(String date) {
        String d = date != null ? date.trim() : "";
        if (d.isEmpty()) return "unknown_date";

        Date parsed = parseDateAny(d);
        if (parsed != null) {
            SimpleDateFormat out = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            return out.format(parsed);
        }

        // Fallback: strip non-digits, keep first 8-10 chars.
        String digits = d.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) return digits.substring(0, 8);
        return digits.isEmpty() ? "unknown_date" : digits;
    }

    private static Date parseDateAny(String date) {
        // dd/MM/yyyy
        Date d1 = parse(date, "dd/MM/yyyy");
        if (d1 != null) return d1;
        // yyyy-MM-dd
        Date d2 = parse(date, "yyyy-MM-dd");
        if (d2 != null) return d2;
        return null;
    }

    private static String normalizeTime(String time) {
        String t = time != null ? time.trim().toUpperCase(Locale.getDefault()) : "";
        if (t.isEmpty()) return "unknown_time";

        Date parsed = parseTimeAny(t);
        if (parsed != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(parsed);
            int hh = c.get(Calendar.HOUR_OF_DAY);
            int mm = c.get(Calendar.MINUTE);
            return String.format(Locale.getDefault(), "%02d%02d", hh, mm);
        }

        // Fallback: strip non-digits.
        String digits = t.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) return digits.substring(0, 4);
        return digits.isEmpty() ? "unknown_time" : digits;
    }

    private static Date parseTimeAny(String time) {
        Date d1 = parse(time, "HH:mm");
        if (d1 != null) return d1;
        Date d2 = parse(time, "H:mm");
        if (d2 != null) return d2;
        Date d3 = parse(time, "hh:mm a");
        if (d3 != null) return d3;
        Date d4 = parse(time, "h:mm a");
        if (d4 != null) return d4;
        Date d5 = parse(time, "h a");
        if (d5 != null) return d5;
        return null;
    }

    private static Date parse(String value, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            sdf.setLenient(false);
            return sdf.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }
}
