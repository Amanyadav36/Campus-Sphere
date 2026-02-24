package com.example.campus_sphere;

import android.os.Bundle;
import android.os.CountDownTimer; // Import Timer
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.Intent;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String SEND_OTP_URL = "https://fkiahnsldyerpyijxsyn.supabase.co/functions/v1/send-otp";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZraWFobnNsZHllcnB5aWp4c3luIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4MjUxMzcsImV4cCI6MjA4MTQwMTEzN30.UMev844BDXHKfBeJZ2iStpabTkY4gC-Eh8sgvqZWZJw";

    private EditText emailInput;
    private Button resetBtn;
    private TextView backToLogin;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.forgotEmail);
        resetBtn = findViewById(R.id.resetBtn);
        backToLogin = findViewById(R.id.backToLogin);

        resetBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 1: Check if user exists first
            checkEmailAndSend(email);
        });

        backToLogin.setOnClickListener(v -> finish());
    }

    private void checkEmailAndSend(String email) {
        resetBtn.setEnabled(false);
        resetBtn.setText("Checking...");

        // 🔥 This checks if the email exists in Firebase
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "Email is not registered!", Toast.LENGTH_LONG).show();
                        resetBtn.setEnabled(true);
                        resetBtn.setText("Send Reset Link");
                    } else {
                        sendResetLink(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetBtn.setEnabled(true);
                    resetBtn.setText("Send Reset Link");
                });
    }

    private void sendResetLink(String email) {
        resetBtn.setEnabled(false);
        resetBtn.setText("Sending Code...");

        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("name", "User");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SEND_OTP_URL)
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String msg = e.getMessage();
                runOnUiThread(() -> {
                    resetBtn.setEnabled(true);
                    resetBtn.setText("Send Reset Link");
                    Toast.makeText(ForgotPasswordActivity.this, "Network Error: " + msg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                String responseData = "";
                int codeStatus = response.code();
                try {
                    if (response.body() != null) {
                        responseData = response.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                final String finalResponseData = responseData;
                runOnUiThread(() -> {
                    resetBtn.setEnabled(true);
                    resetBtn.setText("Send Reset Link");

                    if (response.isSuccessful()) {
                        // Pass data to OTP Verification Screen
                        Intent intent = new Intent(ForgotPasswordActivity.this, OtpVerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("is_forgot_password", true); // Flag for reuse
                        startActivity(intent);
                        startCooldownTimer();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Failed " + codeStatus + ": " + finalResponseData, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // 🕒 15-Second Timer Logic
    private void startCooldownTimer() {
        new CountDownTimer(15000, 1000) { // 15000ms = 15 seconds

            public void onTick(long millisUntilFinished) {
                resetBtn.setEnabled(false); // Keep disabled
                resetBtn.setText("Resend in " + millisUntilFinished / 1000 + "s");
                // Optional: Change button color to grey to show it's disabled
                resetBtn.setAlpha(0.5f);
            }

            public void onFinish() {
                resetBtn.setEnabled(true); // Re-enable
                resetBtn.setText("Send Reset Link");
                resetBtn.setAlpha(1.0f); // Restore color
            }

        }.start();
    }
}
