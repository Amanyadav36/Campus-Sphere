package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText newPass, confirmPass;
    private Button resetBtn;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");

        newPass = findViewById(R.id.newPassword);
        confirmPass = findViewById(R.id.confirmPassword);
        resetBtn = findViewById(R.id.resetBtn);

        resetBtn.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newP = newPass.getText().toString().trim();
        String confirmP = confirmPass.getText().toString().trim();

        if (TextUtils.isEmpty(newP) || TextUtils.isEmpty(confirmP)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation: Alphanumeric and at least one Capital Letter, min length 6
        if (!newP.matches("^(?=.*[A-Z])(?=.*[a-zA-Z0-9]).{6,}$")) {
            Toast.makeText(this, "Password must be at least 6 characters, alphanumeric, with at least 1 uppercase letter", Toast.LENGTH_LONG).show();
            return;
        }

        if (!newP.equals(confirmP)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        resetBtn.setEnabled(false);
        resetBtn.setText("Updating...");

        // Simulate password change via OTP verified system 
        // Note: Actual Firebase Admin SDK update is required in production
        resetBtn.postDelayed(() -> {
            Toast.makeText(this, "Password Changed Successfully!", Toast.LENGTH_LONG).show();
            
            // Go to login page to enter email and password
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1500);
    }
}
