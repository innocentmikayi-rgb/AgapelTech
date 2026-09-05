package com.agapeltech.myapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
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
                        User newUser = new User(uid, email, role);
                        FirebaseHelper.saveUser(newUser, success -> {
                            progressDialog.dismiss();
                            secondaryAuth.signOut(); // Ensure secondary auth is clean
                            if (success) {
                                Toast.makeText(requireContext(), "User added successfully!", Toast.LENGTH_SHORT).show();
                                editNewUserEmail.setText("");
                                editNewUserPassword.setText("");
                            } else {
                                Toast.makeText(requireContext(), "User created but database update failed.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadUsersList() {
        FirebaseDatabase.getInstance().getReference("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<User> users = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        // In case the email wasn't stored previously, we set the UID as the email placeholder or just show role
                        if (user.getEmail() == null) {
                            // If only role exists in the old structure
                            String role = ds.child("role").getValue(String.class);
                            String uid = ds.getKey();
                            String emailPlaceholder = "User (" + (uid != null && uid.length() > 5 ? uid.substring(0, 5) : uid) + ")";
                            user = new User(uid, emailPlaceholder, role != null ? role : "STAFF");
                        }
                        users.add(user);
                    }
                }
                UserAdapter adapter = new UserAdapter(users, new UserAdapter.OnUserActionListener() {
                    @Override
                    public void onUserDelete(User user) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete User")
                                .setMessage("Are you sure you want to delete this user's record?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    FirebaseHelper.deleteUser(user.getUid(), success -> {
                                        if (success) {
                                            Toast.makeText(requireContext(), "User record removed from database", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(), "Failed to delete user record", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton("No", null)
                                .show();
                    }

                    @Override
                    public void onUserEdit(User user) {
                        showEditUserDialog(user);
                    }
                });
                rvUsersList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit User: " + user.getEmail());

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null);
        Spinner spinnerRole = view.findViewById(R.id.spinnerEditUserRole);
        Button btnResetPassword = view.findViewById(R.id.btnResetPassword);

        String[] roles = {"MANAGER", "STAFF"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRole.setAdapter(adapter);
        
        // Set current role
        if ("MANAGER".equals(user.getRole())) spinnerRole.setSelection(0);
        else spinnerRole.setSelection(1);

        btnResetPassword.setOnClickListener(v -> {
            FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Password reset email sent to " + user.getEmail(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        builder.setView(view);
        builder.setPositiveButton("Update Role", (dialog, which) -> {
            String newRole = spinnerRole.getSelectedItem().toString();
            FirebaseHelper.setUserRole(user.getUid(), newRole, success -> {
                if (success) {
                    Toast.makeText(requireContext(), "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update role", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
