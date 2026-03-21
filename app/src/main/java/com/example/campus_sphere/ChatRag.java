package com.example.campus_sphere;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Strict RAG answerer:
 * - Matches user query against Firestore clubs/events
 * - Returns answers ONLY from retrieved data (no LLM hallucinations).
 */
public final class ChatRag {

    private ChatRag() {}

    public static final class RagClub {
        public final String id; // leaderId (legacy) or clubId
        public final String name;
        public final String handle;
        public final String bio;

        public RagClub(String id, String name, String handle, String bio) {
            this.id = safe(id);
            this.name = safe(name);
            this.handle = safe(handle);
            this.bio = safe(bio);
        }
    }

    public static final class RagEvent {
        public final String id;
        public final String title;
        public final String clubId; // creatorId/clubId
        public final String clubName;
        public final String date;
        public final String time;
        public final String venue;
        public final String category;
        public final String price;
        public final String description;

        public RagEvent(
                String id,
                String title,
                String clubId,
                String clubName,
                String date,
                String time,
                String venue,
                String category,
                String price,
                String description
        ) {
            this.id = safe(id);
            this.title = safe(title);
            this.clubId = safe(clubId);
            this.clubName = safe(clubName);
            this.date = safe(date);
            this.time = safe(time);
            this.venue = safe(venue);
            this.category = safe(category);
            this.price = safe(price);
            this.description = safe(description);
        }
    }

    public static String answer(String userQuery, List<RagClub> clubs, List<RagEvent> events) {
        String q = safe(userQuery).trim();
        if (q.isEmpty()) return "Please type a question.";

        // Deterministic "how to" intents (still app-based, no hallucination).
        String qLower = q.toLowerCase(Locale.getDefault());
        if (containsAny(qLower, "how", "how to") && containsAny(qLower, "join") && containsAny(qLower, "club", "clubs")) {
            return "To join a club: open the Clubs tab, select the club, then tap Join.";
        }
        if (containsAny(qLower, "how", "how to") && containsAny(qLower, "register") && containsAny(qLower, "event", "events")) {
            return "To register for an event: open the event from Home, then tap Register.";
        }

        List<String> tokens = tokenize(qLower);

        // Build fast lookup for club names by id.
        Map<String, RagClub> clubById = new HashMap<>();
        for (RagClub c : safeList(clubs)) {
            if (!c.id.isEmpty()) clubById.put(c.id, c);
        }

        // Score clubs/events by keyword overlap.
        List<ScoredClub> scoredClubs = new ArrayList<>();
        for (RagClub c : safeList(clubs)) {
            int score = scoreText(tokens, c.name + " " + c.handle + " " + c.bio);
            if (score > 0) scoredClubs.add(new ScoredClub(c, score));
        }
        Collections.sort(scoredClubs, (a, b) -> Integer.compare(b.score, a.score));

        List<ScoredEvent> scoredEvents = new ArrayList<>();
        for (RagEvent e : safeList(events)) {
            String clubName = e.clubName;
            if (clubName.isEmpty() && !e.clubId.isEmpty()) {
                RagClub c = clubById.get(e.clubId);
                if (c != null) clubName = c.name;
            }
            int score = scoreText(tokens,
                    e.title + " " + e.category + " " + e.venue + " " + e.description + " " + clubName);

            // If query contains a parseable date and event date matches, boost.
            Date qDate = parseDateFromQuery(qLower);
            Date eDate = parseDdMmYyyy(e.date);
            if (qDate != null && eDate != null && isSameDay(qDate, eDate)) {
                score += 3;
            }

            if (score > 0) scoredEvents.add(new ScoredEvent(e, score));
        }
        Collections.sort(scoredEvents, (a, b) -> Integer.compare(b.score, a.score));

        // Direct list intents.
        if (containsAny(qLower, "clubs", "club list", "all clubs")) {
            return buildClubsListAnswer(scoredClubs.isEmpty() ? safeList(clubs) : topClubs(scoredClubs, 20));
        }
        if (containsAny(qLower, "events", "upcoming", "event list", "all events")) {
            return buildEventsListAnswer(scoredEvents.isEmpty() ? safeList(events) : topEvents(scoredEvents, 20));
        }

        // If a club is the best match, show club details + its top events.
        if (!scoredClubs.isEmpty() && scoredClubs.get(0).score >= 2 && (containsAny(qLower, "club", "clubs") || scoredEvents.isEmpty())) {
            RagClub club = scoredClubs.get(0).club;
            List<RagEvent> clubEvents = new ArrayList<>();
            for (RagEvent e : safeList(events)) {
                if (!club.id.isEmpty() && (club.id.equals(e.clubId))) {
                    clubEvents.add(withClubNameResolved(e, club, clubById));
                }
            }
            sortEventsByDateThenTitle(clubEvents);
            return buildClubDetailsAnswer(club, clubEvents);
        }

        // If an event is the best match, show event details (top 3).
        if (!scoredEvents.isEmpty() && scoredEvents.get(0).score >= 2) {
            List<RagEvent> top = topEvents(scoredEvents, 3);
            return buildEventDetailsAnswer(top);
        }

        // Mixed: return top matches from both.
        List<RagClub> topC = topClubs(scoredClubs, 2);
        List<RagEvent> topE = topEvents(scoredEvents, 5);
        if (topC.isEmpty() && topE.isEmpty()) {
            return "No relevant data found.";
        }

        StringBuilder sb = new StringBuilder();
        if (!topC.isEmpty()) {
            sb.append("Clubs:\n");
            for (RagClub c : topC) {
                sb.append(formatClub(c)).append("\n\n");
            }
        }
        if (!topE.isEmpty()) {
            sb.append("Events:\n");
            for (RagEvent e : topE) {
                sb.append(formatEvent(withClubNameResolved(e, null, clubById))).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    private static String buildClubDetailsAnswer(RagClub club, List<RagEvent> clubEvents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Club Details:\n");
        sb.append(formatClub(club)).append("\n\n");
        if (clubEvents == null || clubEvents.isEmpty()) {
            sb.append("No events found for this club.");
            return sb.toString();
        }
        sb.append("Events by this club:\n");
        int limit = Math.min(5, clubEvents.size());
        for (int i = 0; i < limit; i++) {
            sb.append(formatEvent(clubEvents.get(i))).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static String buildEventDetailsAnswer(List<RagEvent> events) {
        if (events == null || events.isEmpty()) return "No relevant data found.";
        StringBuilder sb = new StringBuilder();
        if (events.size() == 1) {
            sb.append("Event Details:\n").append(formatEvent(events.get(0)));
            return sb.toString();
        }
        sb.append("Matching Events:\n");
        for (RagEvent e : events) {
            sb.append(formatEvent(e)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static String buildClubsListAnswer(List<RagClub> clubs) {
        if (clubs == null || clubs.isEmpty()) return "No clubs found.";
        StringBuilder sb = new StringBuilder();
        sb.append("Clubs:\n");
        int limit = Math.min(20, clubs.size());
        for (int i = 0; i < limit; i++) {
            RagClub c = clubs.get(i);
            sb.append("- ").append(!c.name.isEmpty() ? c.name : c.id);
            if (!c.handle.isEmpty()) sb.append(" (@").append(c.handle).append(")");
            sb.append("\n");
        }
        if (clubs.size() > limit) sb.append("...and ").append(clubs.size() - limit).append(" more.");
        return sb.toString().trim();
    }

    private static String buildEventsListAnswer(List<RagEvent> events) {
        if (events == null || events.isEmpty()) return "No events found.";
        List<RagEvent> copy = new ArrayList<>(events);
        sortEventsByDateThenTitle(copy);
        StringBuilder sb = new StringBuilder();
        sb.append("Events:\n");
        int limit = Math.min(20, copy.size());
        for (int i = 0; i < limit; i++) {
            RagEvent e = copy.get(i);
            sb.append("- ").append(!e.title.isEmpty() ? e.title : e.id);
            if (!e.date.isEmpty()) sb.append(" | ").append(e.date);
            if (!e.time.isEmpty()) sb.append(" ").append(e.time);
            if (!e.venue.isEmpty()) sb.append(" | ").append(e.venue);
            if (!e.clubName.isEmpty()) sb.append(" | ").append(e.clubName);
            sb.append("\n");
        }
        if (copy.size() > limit) sb.append("...and ").append(copy.size() - limit).append(" more.");
        return sb.toString().trim();
    }

    private static RagEvent withClubNameResolved(RagEvent e, RagClub clubOverride, Map<String, RagClub> clubById) {
        if (e == null) return null;
        String clubName = e.clubName;
        if (clubOverride != null && clubOverride.name != null && !clubOverride.name.isEmpty()) clubName = clubOverride.name;
        if ((clubName == null || clubName.isEmpty()) && clubById != null && e.clubId != null) {
            RagClub c = clubById.get(e.clubId);
            if (c != null) clubName = c.name;
        }
        return new RagEvent(e.id, e.title, e.clubId, clubName, e.date, e.time, e.venue, e.category, e.price, e.description);
    }

    private static String formatClub(RagClub c) {
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(!c.name.isEmpty() ? c.name : "-").append("\n");
        sb.append("Handle: ").append(!c.handle.isEmpty() ? ("@" + c.handle) : "-").append("\n");
        sb.append("Bio: ").append(!c.bio.isEmpty() ? c.bio : "-").append("\n");
        return sb.toString().trim();
    }

    private static String formatEvent(RagEvent e) {
        if (e == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(!e.title.isEmpty() ? e.title : "-").append("\n");
        sb.append("Club: ").append(!e.clubName.isEmpty() ? e.clubName : (!e.clubId.isEmpty() ? e.clubId : "-")).append("\n");
        sb.append("Category: ").append(!e.category.isEmpty() ? e.category : "-").append("\n");
        sb.append("Date: ").append(!e.date.isEmpty() ? e.date : "-").append("\n");
        sb.append("Time: ").append(!e.time.isEmpty() ? e.time : "-").append("\n");
        sb.append("Venue: ").append(!e.venue.isEmpty() ? e.venue : "-").append("\n");
        sb.append("Price: ").append(!e.price.isEmpty() ? e.price : "-").append("\n");
        sb.append("Description: ").append(!e.description.isEmpty() ? e.description : "-").append("\n");
        return sb.toString().trim();
    }

    private static List<RagClub> topClubs(List<ScoredClub> scored, int n) {
        List<RagClub> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, scored.size()); i++) out.add(scored.get(i).club);
        return out;
    }

    private static List<RagEvent> topEvents(List<ScoredEvent> scored, int n) {
        List<RagEvent> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, scored.size()); i++) out.add(scored.get(i).event);
        return out;
    }

    private static void sortEventsByDateThenTitle(List<RagEvent> events) {
        if (events == null) return;
        Collections.sort(events, (a, b) -> {
            Date da = parseDdMmYyyy(a.date);
            Date db = parseDdMmYyyy(b.date);
            if (da != null && db != null) {
                int cmp = da.compareTo(db);
                if (cmp != 0) return cmp;
            } else if (da != null) {
                return -1;
            } else if (db != null) {
                return 1;
            }
            return safe(a.title).compareToIgnoreCase(safe(b.title));
        });
    }

    private static final class ScoredClub {
        final RagClub club;
        final int score;

        ScoredClub(RagClub club, int score) {
            this.club = club;
            this.score = score;
        }
    }

    private static final class ScoredEvent {
        final RagEvent event;
        final int score;

        ScoredEvent(RagEvent event, int score) {
            this.event = event;
            this.score = score;
        }
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String n : needles) {
            if (n == null) continue;
            if (text.contains(n.toLowerCase(Locale.getDefault()))) return true;
        }
        return false;
    }

    private static List<String> tokenize(String qLower) {
        if (qLower == null) return Collections.emptyList();
        String clean = qLower.replaceAll("[^a-z0-9@/ ]", " ").replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) return Collections.emptyList();

        Set<String> stop = new HashSet<>(Arrays.asList(
                "the","a","an","is","are","am","to","for","of","in","on","at","and","or","with",
                "please","tell","me","about","details","detail","show","give","list","all","any",
                "cdgi","campus","event","events","club","clubs"
        ));

        List<String> out = new ArrayList<>();
        for (String t : clean.split(" ")) {
            if (t.isEmpty()) continue;
            // Normalize "@codingclub" -> "codingclub" so it matches stored handles.
            if (t.startsWith("@") && t.length() > 1) t = t.substring(1);
            if (stop.contains(t)) continue;
            out.add(t);
        }
        return out;
    }

    private static int scoreText(List<String> tokens, String text) {
        if (tokens == null || tokens.isEmpty()) return 0;
        String hay = safe(text).toLowerCase(Locale.getDefault());
        if (hay.isEmpty()) return 0;
        int score = 0;
        for (String t : tokens) {
            if (t == null || t.isEmpty()) continue;
            if (hay.contains(t)) score++;
        }
        return score;
    }

    private static Date parseDdMmYyyy(String date) {
        String d = safe(date).trim();
        if (d.isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        try {
            return sdf.parse(d);
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date parseDateFromQuery(String qLower) {
        if (qLower == null) return null;

        // dd/MM/yyyy
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})").matcher(qLower);
        if (m.find()) {
            String d = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            return parseDdMmYyyy(d);
        }

        // "today" / "tomorrow"
        if (qLower.contains("today")) return startOfDay(new Date());
        if (qLower.contains("tomorrow")) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 1);
            return startOfDay(c.getTime());
        }

        return null;
    }

    private static Date startOfDay(Date d) {
        if (d == null) return null;
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static boolean isSameDay(Date a, Date b) {
        if (a == null || b == null) return false;
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private static <T> List<T> safeList(List<T> in) {
        return in != null ? in : Collections.emptyList();
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
