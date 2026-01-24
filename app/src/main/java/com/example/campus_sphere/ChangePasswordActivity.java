package com.example.campus_sphere;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentPass, newPass, confirmPass;
    private Button changeBtn;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        currentPass = findViewById(R.id.currentPassword);
        newPass = findViewById(R.id.newPassword);
        confirmPass = findViewById(R.id.confirmPassword);
        changeBtn = findViewById(R.id.changePassBtn);

        user = FirebaseAuth.getInstance().getCurrentUser();

        changeBtn.setOnClickListener(v -> attemptPasswordChange());
    }

    private void attemptPasswordChange() {
        String current = currentPass.getText().toString().trim();
        String newP = newPass.getText().toString().trim();
        String confirmP = confirmPass.getText().toString().trim();

        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newP) || TextUtils.isEmpty(confirmP)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newP.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newP.equals(confirmP)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user != null && user.getEmail() != null) {
            changeBtn.setEnabled(false);
            changeBtn.setText("Updating...");

            // 1. RE-AUTHENTICATE USER (Security Requirement)
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 2. UPDATE PASSWORD
                    user.updatePassword(newP).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, "Password Updated Successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity
                        } else {
                            changeBtn.setEnabled(true);
                            changeBtn.setText("Update Password");
                            Toast.makeText(ChangePasswordActivity.this, "Update Failed: " + task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    changeBtn.setEnabled(true);
                    changeBtn.setText("Update Password");
                    Toast.makeText(ChangePasswordActivity.this, "Incorrect Current Password", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}