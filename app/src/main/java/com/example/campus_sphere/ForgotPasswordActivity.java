package com.example.campus_sphere;

import android.os.Bundle;
import android.os.CountDownTimer; // Import Timer
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button resetBtn;
    private TextView backToLogin;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

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
        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    List<String> signInMethods = result.getSignInMethods();

                    // If list is empty, user does not exist
                    if (signInMethods == null || signInMethods.isEmpty()) {
                        Toast.makeText(this, "Email is not registered!", Toast.LENGTH_LONG).show();
                        resetBtn.setEnabled(true);
                        resetBtn.setText("Send Reset Link");
                    } else {
                        // User exists -> Send the email
                        sendResetLink(email);
                    }
                })
                .addOnFailureListener(e -> {
                    // Usually happens if 'Email Enumeration Protection' is ON in console
                    Toast.makeText(this, "Error checking email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetBtn.setEnabled(true);
                    resetBtn.setText("Send Reset Link");
                });
    }

    private void sendResetLink(String email) {
        resetBtn.setText("Sending...");

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Reset link sent! Check your email.", Toast.LENGTH_LONG).show();

                    // ✅ Step 2: Start 15-second Cooldown Timer
                    startCooldownTimer();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetBtn.setEnabled(true);
                    resetBtn.setText("Send Reset Link");
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