package com.example.campus_sphere;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.EditText;
import android.widget.ImageButton;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.io.IOException;

public class ChatBotActivity extends AppCompatActivity {

    private static final String AI_URL = BuildConfig.SUPABASE_URL + "/functions/v1/campus-ai";
    private static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendBtn;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.chatInput);
        sendBtn = findViewById(R.id.sendBtn);

        adapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addMessage("Hello! I am your CDGI Campus Assistant. Ask me anything about our clubs or upcoming events.", false);

        sendBtn.setOnClickListener(v -> {
            String q = messageInput.getText().toString().trim();
            if (q.isEmpty()) return;

            addMessage(q, true);
            messageInput.setText("");

            int placeholderIndex = addMessage("Thinking...", false);
            sendBtn.setEnabled(false);

            answerFromFirestoreStrict(q, answer -> {
                updateMessage(placeholderIndex, answer);
                sendBtn.setEnabled(true);
            });
        });
    }

    private int addMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        int idx = messageList.size() - 1;
        adapter.notifyItemInserted(idx);
        recyclerView.smoothScrollToPosition(idx);
        return idx;
    }

    private void updateMessage(int index, String newText) {
        if (index < 0 || index >= messageList.size()) return;
        messageList.get(index).text = newText;
        adapter.notifyItemChanged(index);
        recyclerView.smoothScrollToPosition(index);
    }

    private interface ResultCallback<T> {
        void onResult(T value);
    }

    /**
     * Strict RAG: answers ONLY from Firestore data (no LLM generation).
     * If no matching club/event exists, returns "No relevant data found."
     */
    private void answerFromFirestoreStrict(String userQuery, ResultCallback<String> cb) {
        fetchClubsMerged(clubs -> fetchEvents(events -> {
            Map<String, ChatRag.RagClub> clubById = new HashMap<>();
            for (ChatRag.RagClub c : clubs) {
                if (c != null && c.id != null && !c.id.trim().isEmpty()) clubById.put(c.id, c);
            }

            List<ChatRag.RagEvent> enrichedEvents = new ArrayList<>();
            for (ChatRag.RagEvent e : events) {
                String clubName = e.clubName;
                if ((clubName == null || clubName.trim().isEmpty()) && e.clubId != null) {
                    ChatRag.RagClub c = clubById.get(e.clubId);
                    if (c != null) clubName = c.name;
                }
                enrichedEvents.add(new ChatRag.RagEvent(
                        e.id, e.title, e.clubId,
                        clubName != null ? clubName : "",
                        e.date, e.time, e.venue, e.category, e.price, e.description
                ));
            }

            String answer = ChatRag.answer(userQuery, clubs, enrichedEvents);
            cb.onResult(answer);
        }));
    }

    private void fetchClubsMerged(ResultCallback<List<ChatRag.RagClub>> cb) {
        Map<String, ChatRag.RagClub> byLeaderId = new HashMap<>();
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(2);

        Runnable finish = () -> cb.onResult(new ArrayList<>(byLeaderId.values()));

        // Preferred: clubs collection
        db.collection("clubs").limit(500).get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String leaderId = doc.getString("leaderId");
                        if (leaderId == null || leaderId.trim().isEmpty()) leaderId = doc.getId();
                        if (leaderId == null) continue;

                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = doc.getString("clubName");
                        String handle = doc.getString("handle");
                        if (handle == null || handle.trim().isEmpty()) handle = doc.getString("clubHandle");
                        String bio = doc.getString("bio");
                        if (bio == null || bio.trim().isEmpty()) bio = doc.getString("clubBio");

                        byLeaderId.put(leaderId, new ChatRag.RagClub(leaderId, safe(name), safe(handle), safe(bio)));
                    }
                    if (remaining.decrementAndGet() == 0) finish.run();
                })
                .addOnFailureListener(e -> {
                    if (remaining.decrementAndGet() == 0) finish.run();
                });

        // Legacy: leaders in users collection
        db.collection("users").whereEqualTo("role", "leader").limit(500).get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String leaderId = doc.getId();
                        if (leaderId == null) continue;

                        String name = doc.getString("clubName");
                        if (name == null || name.trim().isEmpty()) name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = doc.getString("email");

                        String handle = doc.getString("clubHandle");
                        String bio = doc.getString("clubBio");

                        ChatRag.RagClub existing = byLeaderId.get(leaderId);
                        if (existing == null || existing.name == null || existing.name.trim().isEmpty()) {
                            byLeaderId.put(leaderId, new ChatRag.RagClub(leaderId, safe(name), safe(handle), safe(bio)));
                        }
                    }
                    if (remaining.decrementAndGet() == 0) finish.run();
                })
                .addOnFailureListener(e -> {
                    if (remaining.decrementAndGet() == 0) finish.run();
                });
    }

    private void fetchEvents(ResultCallback<List<ChatRag.RagEvent>> cb) {
        db.collection("events").limit(500).get()
                .addOnSuccessListener(snapshot -> cb.onResult(mapEvents(snapshot)))
                .addOnFailureListener(e -> cb.onResult(new ArrayList<>()));
    }

    private List<ChatRag.RagEvent> mapEvents(QuerySnapshot snapshot) {
        List<ChatRag.RagEvent> out = new ArrayList<>();
        if (snapshot == null) return out;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String eventId = doc.getString("eventId");
            if (eventId == null || eventId.trim().isEmpty()) eventId = doc.getId();

            String title = doc.getString("title");
            String category = doc.getString("category");
            String date = doc.getString("date");
            String time = doc.getString("time");
            String venue = doc.getString("venue");
            String price = doc.getString("price");
            String description = doc.getString("description");

            String clubId = doc.getString("clubId");
            if (clubId == null || clubId.trim().isEmpty()) clubId = doc.getString("creatorId");

            out.add(new ChatRag.RagEvent(
                    safe(eventId),
                    safe(title),
                    safe(clubId),
                    "", // resolved later from clubs
                    safe(date),
                    safe(time),
                    safe(venue),
                    safe(category),
                    safe(price),
                    safe(description)
            ));
        }
        return out;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private void callAi(String question, String data, ResultCallback<String> cb) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("question", question);
            json.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(AI_URL)
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> cb.onResult("Network error. Please try again."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String resStr = response.body().string();
                    try {
                        JSONObject resJson = new JSONObject(resStr);
                        String reply = resJson.optString("reply", "I'm not sure how to answer that.");
                        runOnUiThread(() -> cb.onResult(reply));
                    } catch (Exception e) {
                        runOnUiThread(() -> cb.onResult("Error processing response."));
                    }
                } else {
                    runOnUiThread(() -> cb.onResult("No response from assistant."));
                }
            }
        });
    }
}
