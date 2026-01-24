package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

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

        // Send Reset Link Logic
        resetBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
                return;
            }

            resetPassword(email);
        });

        // Back Button Logic
        backToLogin.setOnClickListener(v -> finish()); // Just closes this activity to go back
    }

    private void resetPassword(String email) {
        resetBtn.setEnabled(false);
        resetBtn.setText("Sending...");

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                    resetBtn.setText("Link Sent");
                    // Optional: Navigate back to login automatically
                    // finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetBtn.setEnabled(true);
                    resetBtn.setText("Send Reset Link");
                });
    }
}