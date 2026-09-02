package com.agapeltech.myapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class UserManagementFragment extends Fragment {

    private EditText editNewUserEmail, editNewUserPassword;
    private Spinner spinnerNewUserRole;
    private Button btnAddUser;
    private RecyclerView rvUsersList;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_management, container, false);

        initViews(view);
        loadUsersList();

        return view;
    }

    private void initViews(View v) {
        editNewUserEmail = v.findViewById(R.id.editNewUserEmail);
        editNewUserPassword = v.findViewById(R.id.editNewUserPassword);
        spinnerNewUserRole = v.findViewById(R.id.spinnerNewUserRole);
        btnAddUser = v.findViewById(R.id.btnAddUser);
        rvUsersList = v.findViewById(R.id.rvUsersList);
        rvUsersList.setLayoutManager(new LinearLayoutManager(requireContext()));

        String[] roles = {"MANAGER", "STAFF"};
        spinnerNewUserRole.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles));

        progressDialog = new ProgressDialog(requireContext());

        btnAddUser.setOnClickListener(v1 -> createNewUser());
    }

    private void createNewUser() {
        String email = editNewUserEmail.getText().toString().trim();
        String password = editNewUserPassword.getText().toString().trim();
        String role = spinnerNewUserRole.getSelectedItem().toString();

        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(requireContext(), "Valid email and 6+ char password required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Creating user account...");
        progressDialog.show();

        // Secondary Firebase app instance to avoid logging out the current admin
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        FirebaseApp secondaryApp = null;
        try {
            secondaryApp = FirebaseApp.getInstance("secondary");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.initializeApp(requireContext(), options, "secondary");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        FirebaseHelper.setUserRole(uid, role, success -> {
                            progressDialog.dismiss();
                            secondaryAuth.signOut(); // Ensure secondary auth is clean
                            if (success) {
                                Toast.makeText(requireContext(), "User added successfully!", Toast.LENGTH_SHORT).show();
                                editNewUserEmail.setText("");
                                editNewUserPassword.setText("");
                            } else {
                                Toast.makeText(requireContext(), "User created but role failed.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadUsersList() {
        // Optional: Implement a simple adapter to show existing users from /users in database
        FirebaseDatabase.getInstance().getReference("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Here you would populate the RecyclerView
                // For brevity, skipping for now unless specifically requested
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
