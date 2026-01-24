package com.example.campus_sphere;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatBotActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendBtn;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    // 🔥 SECURITY UPGRADE: Use Supabase URL, NOT Gemini URL
    private static final String SUPABASE_CHAT_URL = "https://fkiahnsldyerpyijxsyn.supabase.co/functions/v1/campus-ai";

    // 🔥 Same Anon Key you used for OTP (Safe to be public-ish, it's restricted by policies)
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZraWFobnNsZHllcnB5aWp4c3luIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4MjUxMzcsImV4cCI6MjA4MTQwMTEzN30.UMev844BDXHKfBeJZ2iStpabTkY4gC-Eh8sgvqZWZJw";

    private static final String APP_CONTEXT =
            "You are 'Campus Sphere AI'. Answer questions about Chameli Devi Group of Institutions. Keep it short.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.chatInput);
        sendBtn = findViewById(R.id.sendBtn);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addMessage("Hello! I am secure and ready to help.", false);

        sendBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                addMessage(msg, true);
                messageInput.setText("");
                callSupabaseAI(msg);
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
    }

    private void callSupabaseAI(String userQuery) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject jsonBody = new JSONObject();
        try {
            // Send simpler JSON to Supabase
            jsonBody.put("message", userQuery);
            jsonBody.put("context", APP_CONTEXT);
        } catch (Exception e) { return; }

        Request request = new Request.Builder()
                .url(SUPABASE_CHAT_URL)
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY) // Auth Header
                .addHeader("apikey", SUPABASE_ANON_KEY) // API Key Header
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addMessage("Connection Error.", false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Supabase sends back: { "reply": "The AI response text" }
                        String aiReply = jsonResponse.getString("reply");

                        runOnUiThread(() -> addMessage(aiReply, false));
                    } catch (Exception e) {
                        runOnUiThread(() -> addMessage("Error reading response.", false));
                    }
                } else {
                    runOnUiThread(() -> addMessage("Server Error: " + response.code(), false));
                }
            }
        });
    }
}