package com.agapeltech.myapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        
        // Auto-login check
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Authenticating...");

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRoleAndProceed(user.getUid(), email);
                        }
                    } else {
                        progressDialog.dismiss();
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        Toast.makeText(LoginActivity.this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserRoleAndProceed(String uid, String email) {
        // Super Admin Bypass for setup
        if ("innocentmikayi@gmail.com".equalsIgnoreCase(email)) {
            saveSessionAndProceed("MANAGER", email);
            return;
        }

        FirebaseHelper.getUserRole(uid, role -> {
            if (role == null || role.isEmpty() || "NONE".equals(role)) {
                progressDialog.dismiss();
                mAuth.signOut(); // Block access
                Toast.makeText(this, "Account pending approval. Please contact Admin.", Toast.LENGTH_LONG).show();
                return;
            }
            saveSessionAndProceed(role, email);
        });
    }

    private void saveSessionAndProceed(String role, String email) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", email);
        editor.putString("role", role);
        editor.apply();

        if (progressDialog.isShowing()) progressDialog.dismiss();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
    }
}
