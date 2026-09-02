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
    private Button btnLogin, btnRegister;
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
        btnRegister = findViewById(R.id.btnRegister);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Authenticating...");

        btnLogin.setOnClickListener(v -> performLogin());
        btnRegister.setOnClickListener(v -> showRegistrationDialog());
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
                        fetchUserRoleAndProceed(user.getUid(), email);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserRoleAndProceed(String uid, String email) {
        FirebaseHelper.getUserRole(uid, role -> {
            // Save session locally
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", email);
            editor.putString("role", role);
            editor.apply();

            progressDialog.dismiss();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }

    private void showRegistrationDialog() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter desired email and password first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] roles = {"MANAGER", "STAFF"};
        new AlertDialog.Builder(this)
                .setTitle("Select User Role")
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = roles[which];
                    performRegistration(email, pass, selectedRole);
                })
                .show();
    }

    private void performRegistration(String email, String pass, String role) {
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        FirebaseHelper.setUserRole(uid, role, success -> {
                            progressDialog.dismiss();
                            if (success) {
                                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                fetchUserRoleAndProceed(uid, email);
                            } else {
                                Toast.makeText(this, "Account created but role failed to save.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
    }
}
