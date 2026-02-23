package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    // UI
    private EditText email, password;
    private Button loginBtn;
    private TextView signupRedirect;
    private SignInButton googleSignInBtn;

    // Firebase
    private FirebaseAuth auth;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.emailLogin);
        password = findViewById(R.id.passwordLogin);
        loginBtn = findViewById(R.id.loginBtn);
        signupRedirect = findViewById(R.id.signupRedirect);
        googleSignInBtn = findViewById(R.id.googleSignInBtn);

        loginBtn.setOnClickListener(v -> loginWithEmail());

        TextView forgotPass = findViewById(R.id.forgotPassword);

        forgotPass.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );

        signupRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class))
        );

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInBtn.setOnClickListener(v -> {

            // 🔥 CLEAR ANY OTP STATE — GOOGLE DOES NOT NEED OTP
            getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    .edit()
                    .remove("current_flow")
                    .remove("otp_verified")
                    .apply();

            startActivityForResult(
                    googleSignInClient.getSignInIntent(),
                    RC_SIGN_IN
            );
        });

    }

    // ---------------- EMAIL LOGIN ----------------

    private void loginWithEmail() {
        String emailStr = email.getText().toString().trim();
        String passStr = password.getText().toString().trim();

        if (emailStr.isEmpty() || passStr.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(emailStr, passStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkRoleAndRedirect(auth.getCurrentUser().getUid());
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";

                        // 🎯 USER NOT FOUND HANDLING
                        if (msg.toLowerCase().contains("no user record")
                                || msg.toLowerCase().contains("user does not exist")) {
                            Toast.makeText(this,
                                    "User not found. Please sign up first.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    // ---------------- GOOGLE RESULT ----------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account =
                        task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        // 🔥 ABSOLUTELY ENSURE OTP IS NOT REQUIRED
                        getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                .edit()
                                .remove("current_flow")
                                .remove("otp_verified")
                                .apply();

                        checkRoleAndRedirect(auth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(this, "Google auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // ---------------- ROLE CHECK & REDIRECT ----------------

    private void checkRoleAndRedirect(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {

                    // 🔴 NEW USER → DETAILS PAGE
                    if (!document.exists()) {
                        startActivity(new Intent(this, UserDetailsActivity.class));
                        finish();
                        return;
                    }

                    Boolean profileCompleted = document.getBoolean("profileCompleted");
                    String role = document.getString("role");

                    // 🔴 Profile not completed
                    if (profileCompleted == null || !profileCompleted) {
                        startActivity(new Intent(this, UserDetailsActivity.class));
                        finish();
                        return;
                    }

                    // ✅ Role based routing
                    navigateToDashboard(role);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    // Helper to create a user if they login with Google but have no database entry
    private void createDefaultUser(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = auth.getCurrentUser().getEmail();
        String name = auth.getCurrentUser().getDisplayName(); // Try to get name from Google

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name != null ? name : "New User");
        userMap.put("email", email);
        userMap.put("role", "user"); // Default role

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> navigateToDashboard("user"))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create user record", Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if (role == null) role = "user";

        if ("admin".equals(role)) {
            intent = new Intent(LoginActivity.this, AdminActivity.class);
        } else if ("leader".equals(role)) {
            intent = new Intent(LoginActivity.this, LeaderActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ---------------- AUTO LOGIN ----------------

    @Override
    protected void onStart() {
        super.onStart();
        // ✅ CHANGED: Removed OTP check. If user is logged in, just go.
        if (auth.getCurrentUser() != null) {
            checkRoleAndRedirect(auth.getCurrentUser().getUid());
        }
    }
}
