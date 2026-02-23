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

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText email, password;
    private Button signupBtn;
    private TextView loginRedirect;
    private FirebaseAuth auth;

    // Supabase URL (Same as before)
    private static final String SEND_OTP_URL = "https://fkiahnsldyerpyijxsyn.supabase.co/functions/v1/send-otp";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZraWFobnNsZHllcnB5aWp4c3luIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4MjUxMzcsImV4cCI6MjA4MTQwMTEzN30.UMev844BDXHKfBeJZ2iStpabTkY4gC-Eh8sgvqZWZJw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        // 1. Initialize Only Email & Password (Matches your XML)
        email = findViewById(R.id.emailSignup);
        password = findViewById(R.id.passwordSignup);
        signupBtn = findViewById(R.id.signupBtn);
        loginRedirect = findViewById(R.id.loginRedirect);

        signupBtn.setOnClickListener(v -> {
            String emailTxt = email.getText().toString().trim();
            String passTxt = password.getText().toString().trim();

            if (TextUtils.isEmpty(emailTxt) || TextUtils.isEmpty(passTxt)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTxt).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (passTxt.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send OTP
            sendOtp(emailTxt, passTxt);
        });

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void sendOtp(String emailStr, String passStr) {
        signupBtn.setEnabled(false);
        signupBtn.setText("Sending Code...");

        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("email", emailStr);
            json.put("name", "New User"); // Placeholder name (User fills real name in next Profile screen)
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
                runOnUiThread(() -> {
                    signupBtn.setEnabled(true);
                    signupBtn.setText("Sign Up");
                    Toast.makeText(SignupActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    signupBtn.setEnabled(true);
                    signupBtn.setText("Sign Up");

                    if (response.isSuccessful()) {
                        // Pass data to OTP Verification Screen
                        Intent intent = new Intent(SignupActivity.this, OtpVerificationActivity.class);
                        intent.putExtra("email", emailStr);
                        intent.putExtra("password", passStr);
                        intent.putExtra("name", "Student"); // Default placeholder
                        startActivity(intent);
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}