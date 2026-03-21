package com.example.campus_sphere;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.AggregateSource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private Spinner spinnerClub;
    private Spinner spinnerEvent;
    private TextView tvClubEventCount;
    private TextView tvEventRegCount;
    private TextView tvStatus;
    private Button btnDownloadCsv;

    private BarChart chartEventsPerClub;
    private BarChart chartRegsPerEvent;
    private PieChart chartPaymentSplit;

    private final List<IdName> clubs = new ArrayList<>();
    private final List<EventItem> events = new ArrayList<>();

    private ArrayAdapter<String> clubAdapter;
    private ArrayAdapter<String> eventAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        db = FirebaseFirestore.getInstance();

        spinnerClub = findViewById(R.id.spinnerClub);
        spinnerEvent = findViewById(R.id.spinnerEvent);
        tvClubEventCount = findViewById(R.id.tvClubEventCount);
        tvEventRegCount = findViewById(R.id.tvEventRegCount);
        tvStatus = findViewById(R.id.tvStatus);
        btnDownloadCsv = findViewById(R.id.btnDownloadCsv);
        chartEventsPerClub = findViewById(R.id.chartEventsPerClub);
        chartRegsPerEvent = findViewById(R.id.chartRegsPerEvent);
        chartPaymentSplit = findViewById(R.id.chartPaymentSplit);

        setupBarChart(chartEventsPerClub, "Events per club");
        setupBarChart(chartRegsPerEvent, "Registrations per event");
        setupPieChart(chartPaymentSplit, "Payment split");

        clubAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerClub.setAdapter(clubAdapter);

        eventAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerEvent.setAdapter(eventAdapter);

        spinnerClub.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position < 0 || position >= clubs.size()) return;
                IdName club = clubs.get(position);
                loadClubAnalytics(club.id);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerEvent.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position < 0 || position >= events.size()) return;
                EventItem event = events.get(position);
                loadEventRegistrationsCount(event.eventId);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnDownloadCsv.setOnClickListener(v -> {
            int pos = spinnerEvent.getSelectedItemPosition();
            if (pos < 0 || pos >= events.size()) {
                Toast.makeText(this, "Select an event", Toast.LENGTH_SHORT).show();
                return;
            }
            downloadRegistrationsCsv(events.get(pos));
        });

        loadClubs();
    }

    private void setStatus(String text) {
        tvStatus.setText(text != null ? text : "");
    }

    // ---------------------------------
    // 1) Total events per club
    // ---------------------------------

    private void loadClubs() {
        setStatus("Loading clubs...");
        clubs.clear();
        clubAdapter.clear();

        // Merge clubs from:
        // 1) `clubs` collection (preferred)
        // 2) legacy storage: leader user docs (`users` where role=leader)
        Map<String, String> nameByLeaderId = new HashMap<>();
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(2);

        Runnable finish = () -> {
            clubs.clear();
            for (Map.Entry<String, String> e : nameByLeaderId.entrySet()) {
                String id = e.getKey();
                String name = e.getValue();
                if (name == null || name.trim().isEmpty()) name = id;
                clubs.add(new IdName(id, name));
            }
            afterClubsLoaded();
        };

        db.collection("clubs").get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // Ensure IDs match events.creatorId: use leaderId if present.
                        String leaderId = doc.getString("leaderId");
                        if (leaderId == null || leaderId.trim().isEmpty()) leaderId = doc.getId();

                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = doc.getString("clubName");
                        if (name == null || name.trim().isEmpty()) name = leaderId;

                        if (!nameByLeaderId.containsKey(leaderId)) {
                            nameByLeaderId.put(leaderId, name);
                        }
                    }
                    if (remaining.decrementAndGet() == 0) finish.run();
                })
                .addOnFailureListener(e -> {
                    if (remaining.decrementAndGet() == 0) finish.run();
                });

        db.collection("users").whereEqualTo("role", "leader").get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String leaderId = doc.getId();
                        String name = doc.getString("clubName");
                        if (name == null || name.trim().isEmpty()) name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = doc.getString("email");
                        if (name == null || name.trim().isEmpty()) name = leaderId;

                        String existing = nameByLeaderId.get(leaderId);
                        if (existing == null || existing.trim().isEmpty() || existing.equals(leaderId)) {
                            nameByLeaderId.put(leaderId, name);
                        } else if (!existing.equals(name) && existing.length() < name.length()) {
                            // Prefer more descriptive name if we got only a short placeholder earlier.
                            nameByLeaderId.put(leaderId, name);
                        }
                    }
                    if (remaining.decrementAndGet() == 0) finish.run();
                })
                .addOnFailureListener(e -> {
                    if (remaining.decrementAndGet() == 0) finish.run();
                });
    }

    private void afterClubsLoaded() {
        Collections.sort(clubs, Comparator.comparing(a -> a.name.toLowerCase(Locale.getDefault())));
        clubAdapter.clear();
        for (IdName c : clubs) clubAdapter.add(c.name);
        clubAdapter.notifyDataSetChanged();

        if (clubs.isEmpty()) {
            setStatus("No clubs found.");
            renderEmptyChart(chartEventsPerClub, "No clubs");
        } else {
            setStatus("");
            spinnerClub.setSelection(0);
            loadEventsPerClubChart();
        }
    }

    private void loadClubAnalytics(String clubId) {
        tvClubEventCount.setText("Events: ...");
        setStatus("Loading club analytics...");

        // Firestore query: events where creatorId == clubId
        db.collection("events")
                .whereEqualTo("creatorId", clubId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> {
                    long c = snap.getCount();
                    if (c > 0) {
                        tvClubEventCount.setText("Events: " + c);
                        setStatus("");
                        loadEventsForClub(clubId);
                        return;
                    }

                    // Fallback if your dataset uses `clubId` field on events.
                    db.collection("events")
                            .whereEqualTo("clubId", clubId)
                            .count()
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener(s2 -> {
                                tvClubEventCount.setText("Events: " + s2.getCount());
                                setStatus("");
                                loadEventsForClub(clubId);
                            })
                            .addOnFailureListener(e -> {
                                tvClubEventCount.setText("Events: -");
                                setStatus("Failed to load events: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setStatus("Failed to load events: " + e.getMessage());
                    tvClubEventCount.setText("Events: -");
                });
    }

    private void loadEventsForClub(String clubId) {
        events.clear();
        eventAdapter.clear();

        db.collection("events")
                .whereEqualTo("creatorId", clubId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        // Fallback to `clubId` field if needed.
                        db.collection("events")
                                .whereEqualTo("clubId", clubId)
                                .get()
                                .addOnSuccessListener(this::renderEventsList)
                                .addOnFailureListener(e -> setStatus("Failed to load events list: " + e.getMessage()));
                        return;
                    }
                    renderEventsList(snapshot);
                })
                .addOnFailureListener(e -> setStatus("Failed to load events list: " + e.getMessage()));
    }

    private void renderEventsList(com.google.firebase.firestore.QuerySnapshot snapshot) {
        events.clear();
        eventAdapter.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String eventId = doc.getString("eventId");
            if (eventId == null || eventId.trim().isEmpty()) eventId = doc.getId();
            String title = doc.getString("title");
            String date = doc.getString("date");
            String time = doc.getString("time");

            String label = (title != null && !title.trim().isEmpty()) ? title.trim() : eventId;
            if (date != null && !date.trim().isEmpty()) label += " | " + date.trim();
            if (time != null && !time.trim().isEmpty()) label += " " + time.trim();

            events.add(new EventItem(eventId, label, title));
        }

        Collections.sort(events, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));
        for (EventItem e : events) eventAdapter.add(e.label);
        eventAdapter.notifyDataSetChanged();

        if (!events.isEmpty()) {
            spinnerEvent.setSelection(0);
            loadRegistrationsPerEventChart();
        } else {
            tvEventRegCount.setText("Registrations: -");
            setStatus("No events found for selected club.");
            renderEmptyChart(chartRegsPerEvent, "No events");
            renderEmptyPie(chartPaymentSplit, "Select an event");
        }
    }

    // ---------------------------------
    // 2) Total registrations per event
    // ---------------------------------

    private void loadEventRegistrationsCount(String eventId) {
        tvEventRegCount.setText("Registrations: ...");
        setStatus("Counting registrations...");

        // Firestore query: tickets where eventId == selected event
        db.collection("tickets")
                .whereEqualTo("eventId", eventId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> {
                    tvEventRegCount.setText("Registrations: " + snap.getCount());
                    setStatus("");
                })
                .addOnFailureListener(e -> {
                    tvEventRegCount.setText("Registrations: -");
                    setStatus("Failed to count registrations: " + e.getMessage());
                });

        loadEventPaymentSplit(eventId);
    }

    // ---------------------------------
    // Charts
    // ---------------------------------

    private void setupBarChart(BarChart chart, String noDataText) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setNoDataText(noDataText != null ? noDataText : "No data");
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getLegend().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(30f);

        chart.setFitBars(true);
    }

    private void setupPieChart(PieChart chart, String noDataText) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setNoDataText(noDataText != null ? noDataText : "No data");
        chart.setUsePercentValues(false);
        chart.setDrawEntryLabels(false);
        chart.getLegend().setEnabled(true);
        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(60f);
    }

    private void renderEmptyChart(BarChart chart, String message) {
        if (chart == null) return;
        chart.clear();
        chart.setNoDataText(message != null ? message : "No data");
        chart.invalidate();
    }

    private void renderEmptyPie(PieChart chart, String message) {
        if (chart == null) return;
        chart.clear();
        chart.setNoDataText(message != null ? message : "No data");
        chart.invalidate();
    }

    private void loadEventsPerClubChart() {
        if (clubs.isEmpty()) {
            renderEmptyChart(chartEventsPerClub, "No clubs");
            return;
        }

        setStatus("Loading events per club...");

        // Build from real data (single scan of events collection).
        db.collection("events")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Integer> countByCreator = new HashMap<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String creatorId = doc.getString("creatorId");
                        if (creatorId == null || creatorId.trim().isEmpty()) {
                            creatorId = doc.getString("clubId");
                        }
                        if (creatorId == null || creatorId.trim().isEmpty()) continue;
                        Integer cur = countByCreator.get(creatorId);
                        countByCreator.put(creatorId, cur == null ? 1 : (cur + 1));
                    }

                    List<ClubCount> counts = new ArrayList<>();
                    for (IdName club : clubs) {
                        int c = 0;
                        Integer v = countByCreator.get(club.id);
                        if (v != null) c = v;
                        counts.add(new ClubCount(club.name, c));
                    }

                    renderEventsPerClubChart(counts);
                    setStatus("");
                })
                .addOnFailureListener(e -> {
                    renderEmptyChart(chartEventsPerClub, "Failed to load");
                    setStatus("Failed to load events: " + e.getMessage());
                });
    }

    private void renderEventsPerClubChart(List<ClubCount> counts) {
        if (chartEventsPerClub == null) return;
        if (counts == null || counts.isEmpty()) {
            renderEmptyChart(chartEventsPerClub, "No data");
            return;
        }

        Collections.sort(counts, (a, b) -> Integer.compare(b.count, a.count));
        if (counts.size() > 10) counts = counts.subList(0, 10);

        List<String> labels = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < counts.size(); i++) {
            ClubCount c = counts.get(i);
            labels.add(shortLabel(c.name));
            entries.add(new BarEntry(i, c.count));
        }

        BarDataSet ds = new BarDataSet(entries, "Events");
        ds.setColor(0xFF0B3D91); // app_primary
        ds.setValueTextColor(0xFF1F2937);
        ds.setValueTextSize(10f);

        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);

        chartEventsPerClub.setData(data);
        chartEventsPerClub.getXAxis().setValueFormatter(new IndexValueFormatter(labels));
        chartEventsPerClub.getXAxis().setLabelCount(labels.size());
        chartEventsPerClub.invalidate();
    }

    private void loadRegistrationsPerEventChart() {
        if (events.isEmpty()) {
            renderEmptyChart(chartRegsPerEvent, "No events");
            return;
        }

        setStatus("Loading registrations per event...");

        // Real-data aggregation: count tickets for ALL events of this club by batching whereIn(eventId).
        List<String> eventIds = new ArrayList<>();
        Map<String, String> labelByEventId = new HashMap<>();
        for (EventItem e : events) {
            if (e == null || e.eventId == null || e.eventId.trim().isEmpty()) continue;
            eventIds.add(e.eventId);
            labelByEventId.put(e.eventId, labelForEvent(e));
        }

        if (eventIds.isEmpty()) {
            renderEmptyChart(chartRegsPerEvent, "No events");
            setStatus("");
            return;
        }

        Map<String, Integer> countByEventId = new HashMap<>();
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < eventIds.size(); i += 10) {
            batches.add(eventIds.subList(i, Math.min(i + 10, eventIds.size())));
        }

        AtomicInteger remaining = new AtomicInteger(batches.size());
        for (List<String> batch : batches) {
            db.collection("tickets")
                    .whereIn("eventId", batch)
                    .get()
                    .addOnSuccessListener(ticketSnap -> {
                        for (DocumentSnapshot t : ticketSnap.getDocuments()) {
                            String eid = t.getString("eventId");
                            if (eid == null) continue;
                            Integer cur = countByEventId.get(eid);
                            countByEventId.put(eid, cur == null ? 1 : (cur + 1));
                        }

                        if (remaining.decrementAndGet() == 0) {
                            List<EventRegCount> counts = new ArrayList<>();
                            for (String eid : eventIds) {
                                int c = 0;
                                Integer v = countByEventId.get(eid);
                                if (v != null) c = v;
                                counts.add(new EventRegCount(labelByEventId.get(eid), c));
                            }

                            Collections.sort(counts, (a, b) -> Integer.compare(b.count, a.count));
                            if (counts.size() > 10) counts = counts.subList(0, 10);
                            renderRegsPerEventChart(counts);
                            setStatus("");
                        }
                    })
                    .addOnFailureListener(ex -> {
                        if (remaining.decrementAndGet() == 0) {
                            List<EventRegCount> counts = new ArrayList<>();
                            for (String eid : eventIds) {
                                int c = 0;
                                Integer v = countByEventId.get(eid);
                                if (v != null) c = v;
                                counts.add(new EventRegCount(labelByEventId.get(eid), c));
                            }
                            Collections.sort(counts, (a, b) -> Integer.compare(b.count, a.count));
                            if (counts.size() > 10) counts = counts.subList(0, 10);
                            renderRegsPerEventChart(counts);
                            setStatus("");
                        }
                    });
        }
    }

    private void loadEventPaymentSplit(String eventId) {
        if (chartPaymentSplit == null) return;
        if (eventId == null || eventId.trim().isEmpty()) {
            renderEmptyPie(chartPaymentSplit, "Select an event");
            return;
        }

        db.collection("tickets")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    long free = 0;
                    long paidVerified = 0;
                    long paidUnverified = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String paymentId = doc.getString("paymentId");
                        boolean paid = paymentId != null && !paymentId.equals("FREE_TICKET");
                        Boolean verified = doc.getBoolean("verified");
                        boolean isVerified = verified != null && verified;

                        if (!paid) {
                            free++;
                        } else if (isVerified) {
                            paidVerified++;
                        } else {
                            paidUnverified++;
                        }
                    }

                    List<PieEntry> entries = new ArrayList<>();
                    if (free > 0) entries.add(new PieEntry((float) free, "Free"));
                    if (paidVerified > 0) entries.add(new PieEntry((float) paidVerified, "Paid Verified"));
                    if (paidUnverified > 0) entries.add(new PieEntry((float) paidUnverified, "Paid Unverified"));

                    if (entries.isEmpty()) {
                        renderEmptyPie(chartPaymentSplit, "No tickets");
                        return;
                    }

                    PieDataSet ds = new PieDataSet(entries, "");
                    List<Integer> colors = new ArrayList<>();
                    colors.add(0xFF94A3B8); // slate for free
                    colors.add(0xFF00B894); // green verified
                    colors.add(0xFFE17055); // orange unverified
                    ds.setColors(colors);
                    ds.setValueTextColor(0xFF1F2937);
                    ds.setValueTextSize(11f);

                    PieData data = new PieData(ds);
                    chartPaymentSplit.setData(data);
                    chartPaymentSplit.invalidate();
                })
                .addOnFailureListener(e -> renderEmptyPie(chartPaymentSplit, "Failed to load"));
    }

    private void renderRegsPerEventChart(List<EventRegCount> counts) {
        if (chartRegsPerEvent == null) return;
        if (counts == null || counts.isEmpty()) {
            renderEmptyChart(chartRegsPerEvent, "No data");
            return;
        }

        Collections.sort(counts, (a, b) -> Integer.compare(b.count, a.count));

        List<String> labels = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < counts.size(); i++) {
            EventRegCount c = counts.get(i);
            labels.add(shortLabel(c.label));
            entries.add(new BarEntry(i, c.count));
        }

        BarDataSet ds = new BarDataSet(entries, "Registrations");
        ds.setColor(0xFF00B894); // app_secondary
        ds.setValueTextColor(0xFF1F2937);
        ds.setValueTextSize(10f);

        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);

        chartRegsPerEvent.setData(data);
        chartRegsPerEvent.getXAxis().setValueFormatter(new IndexValueFormatter(labels));
        chartRegsPerEvent.getXAxis().setLabelCount(labels.size());
        chartRegsPerEvent.invalidate();
    }

    private String labelForEvent(EventItem e) {
        if (e == null) return "";
        if (e.title != null && !e.title.trim().isEmpty()) return e.title.trim();
        if (e.label != null && !e.label.trim().isEmpty()) return e.label.trim();
        return e.eventId;
    }

    private String shortLabel(String text) {
        if (text == null) return "";
        String t = text.trim();
        if (t.length() <= 10) return t;
        return t.substring(0, 10) + "...";
    }

    private static final class ClubCount {
        final String name;
        final int count;

        ClubCount(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    private static final class EventRegCount {
        final String label;
        final int count;

        EventRegCount(String label, int count) {
            this.label = label;
            this.count = count;
        }
    }

    private static final class IndexValueFormatter extends ValueFormatter {
        private final List<String> labels;

        IndexValueFormatter(List<String> labels) {
            this.labels = labels != null ? labels : new ArrayList<>();
        }

        @Override
        public String getFormattedValue(float value) {
            int idx = (int) value;
            if (idx >= 0 && idx < labels.size()) return labels.get(idx);
            return "";
        }
    }

    // ---------------------------------
    // 3) Download registered students CSV
    // ---------------------------------

    private void downloadRegistrationsCsv(EventItem event) {
        btnDownloadCsv.setEnabled(false);
        setStatus("Generating CSV...");

        db.collection("tickets")
                .whereEqualTo("eventId", event.eventId)
                .get()
                .addOnSuccessListener(ticketSnap -> {
                    if (ticketSnap.isEmpty()) {
                        btnDownloadCsv.setEnabled(true);
                        setStatus("No registrations for this event.");
                        return;
                    }

                    Set<String> userIdsSet = new HashSet<>();
                    for (DocumentSnapshot t : ticketSnap.getDocuments()) {
                        String uid = t.getString("userId");
                        if (uid != null && !uid.trim().isEmpty()) userIdsSet.add(uid);
                    }

                    List<String> userIds = new ArrayList<>(userIdsSet);
                    Map<String, DocumentSnapshot> usersById = new HashMap<>();

                    List<List<String>> batches = new ArrayList<>();
                    for (int i = 0; i < userIds.size(); i += 10) {
                        batches.add(userIds.subList(i, Math.min(i + 10, userIds.size())));
                    }

                    if (batches.isEmpty()) {
                        btnDownloadCsv.setEnabled(true);
                        setStatus("No userIds found in tickets.");
                        return;
                    }

                    AtomicInteger remaining = new AtomicInteger(batches.size());

                    for (List<String> batch : batches) {
                        db.collection("users")
                                .whereIn(FieldPath.documentId(), batch)
                                .get()
                                .addOnSuccessListener(userSnap -> {
                                    for (DocumentSnapshot u : userSnap.getDocuments()) {
                                        usersById.put(u.getId(), u);
                                    }

                                    if (remaining.decrementAndGet() == 0) {
                                        buildAndSaveCsv(event, userIds, usersById);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0) {
                                        buildAndSaveCsv(event, userIds, usersById);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    btnDownloadCsv.setEnabled(true);
                    setStatus("Failed to load tickets: " + e.getMessage());
                });
    }

    private void buildAndSaveCsv(EventItem event, List<String> userIds, Map<String, DocumentSnapshot> usersById) {
        try {
            List<CsvUtils.StudentRow> rows = new ArrayList<>();
            for (String uid : userIds) {
                DocumentSnapshot u = usersById.get(uid);
                String name = u != null ? u.getString("name") : "";
                String email = u != null ? u.getString("email") : "";
                String mobile = u != null ? u.getString("mobile") : "";
                String branch = u != null ? u.getString("branch") : "";
                String enrollment = u != null ? u.getString("enrollment") : "";
                String section = u != null ? u.getString("section") : "";
                String year = u != null ? u.getString("year") : "";
                if ((section == null || section.trim().isEmpty()) && year != null) {
                    section = year;
                }
                if ((year == null || year.trim().isEmpty()) && section != null) {
                    year = section;
                }
                rows.add(new CsvUtils.StudentRow(name, email, mobile, branch, enrollment, section, year));
            }

            String csv = CsvUtils.buildStudentCsv(rows);

            String safeTitle = (event.title != null ? event.title : "event").replaceAll("[^a-zA-Z0-9._-]", "_");
            String ts = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            String fileName = "registrations_" + safeTitle + "_" + ts + ".csv";

            Uri uri = FileSaver.saveCsvToDownloads(this, fileName, csv);
            btnDownloadCsv.setEnabled(true);
            setStatus("Saved CSV: " + uri);
            Toast.makeText(this, "CSV saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            btnDownloadCsv.setEnabled(true);
            setStatus("Failed to save CSV: " + e.getMessage());
        }
    }

    private static final class IdName {
        final String id;
        final String name;

        IdName(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class EventItem {
        final String eventId;
        final String label;
        final String title;

        EventItem(String eventId, String label, String title) {
            this.eventId = eventId;
            this.label = label;
            this.title = title;
        }
    }
}
