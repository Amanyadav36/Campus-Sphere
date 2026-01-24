package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OtpVerificationActivity extends AppCompatActivity {

    // UI Components for 6-digit layout
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button verifyBtn;
    private TextView resendText;

    // Data from previous screen
    private String email, password, name;
    private FirebaseAuth auth;

    // Supabase Config
    private static final String VERIFY_OTP_URL = "https://fkiahnsldyerpyijxsyn.supabase.co/functions/v1/smart-processor";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZraWFobnNsZHllcnB5aWp4c3luIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4MjUxMzcsImV4cCI6MjA4MTQwMTEzN30.UMev844BDXHKfBeJZ2iStpabTkY4gC-Eh8sgvqZWZJw";

    // Timer Config
    private static final long COOLDOWN = 30_000; // 30 Seconds
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        auth = FirebaseAuth.getInstance();

        // 1. Initialize UI
        otp1 = findViewById(R.id.otpDigit1);
        otp2 = findViewById(R.id.otpDigit2);
        otp3 = findViewById(R.id.otpDigit3);
        otp4 = findViewById(R.id.otpDigit4);
        otp5 = findViewById(R.id.otpDigit5);
        otp6 = findViewById(R.id.otpDigit6);

        verifyBtn = findViewById(R.id.verifyOtpBtn);
        resendText = findViewById(R.id.resendCode);

        // 2. Setup Auto-Move Logic (The Magic)
        setupOTPInputs(otp1, otp2);
        setupOTPInputs(otp2, otp3);
        setupOTPInputs(otp3, otp4);
        setupOTPInputs(otp4, otp5);
        setupOTPInputs(otp5, otp6);
        setupOTPInputs(otp6, null); // Last one stays focused

        // 3. Get Data from Signup
        Intent intent = getIntent();
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");
        name = intent.getStringExtra("name");

        // 4. Listeners
        verifyBtn.setOnClickListener(v -> verifyOtp());
        resendText.setOnClickListener(v -> resendOtp());

        startResendCooldown();
    }

    // ---------------- HELPER: AUTO MOVE CURSOR ----------------
    private void setupOTPInputs(final EditText current, final EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) {
                    next.requestFocus();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ---------------- VERIFY OTP ----------------

    private void verifyOtp() {
        // Concatenate all 6 boxes
        String code = otp1.getText().toString().trim() +
                otp2.getText().toString().trim() +
                otp3.getText().toString().trim() +
                otp4.getText().toString().trim() +
                otp5.getText().toString().trim() +
                otp6.getText().toString().trim();

        if (code.length() != 6) {
            Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyBtn.setEnabled(false);
        verifyBtn.setText("Verifying...");

        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();

        try {
            json.put("email", email);
            json.put("otp", code);
        } catch (Exception e) { return; }

        Request request = new Request.Builder()
                .url(VERIFY_OTP_URL)
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    verifyBtn.setEnabled(true);
                    verifyBtn.setText("Verify");
                    Toast.makeText(OtpVerificationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                // Fix: Close response body to prevent leaks
                try (ResponseBody body = response.body()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            // OTP Correct -> Create Firebase Account
                            createFirebaseAccount();
                        } else {
                            verifyBtn.setEnabled(true);
                            verifyBtn.setText("Verify");
                            Toast.makeText(OtpVerificationActivity.this, "Invalid Code", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // ---------------- CREATE FIREBASE ACCOUNT ----------------

    private void createFirebaseAccount() {
        verifyBtn.setText("Creating Account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(auth.getCurrentUser().getUid());
                    } else {
                        verifyBtn.setEnabled(true);
                        verifyBtn.setText("Verify");
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(OtpVerificationActivity.this, "Signup Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ---------------- FIRESTORE SAVING ----------------

    private void saveUserToFirestore(String uid) {
        verifyBtn.setText("Saving Data...");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", "user");
        userMap.put("email_verified", true);
        userMap.put("profileCompleted", false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener(v -> {
                    Toast.makeText(OtpVerificationActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(OtpVerificationActivity.this, UserDetailsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    verifyBtn.setEnabled(true);
                    verifyBtn.setText("Verify");
                    Toast.makeText(OtpVerificationActivity.this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------- RESEND LOGIC ----------------

    private void resendOtp() {
        // Call your Resend API here if needed
        Toast.makeText(this, "Code resent!", Toast.LENGTH_SHORT).show();
        startResendCooldown();
    }

    private void startResendCooldown() {
        resendText.setEnabled(false);
        resendText.setTextColor(getResources().getColor(android.R.color.darker_gray));

        timer = new CountDownTimer(COOLDOWN, 1000) {
            public void onTick(long ms) {
                resendText.setText("Resend in " + (ms / 1000) + "s");
            }
            public void onFinish() {
                resendText.setText("Didn't receive a code? Resend");
                resendText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark)); // Or your brand color
                resendText.setEnabled(true);
            }
        }.start();
    }
}