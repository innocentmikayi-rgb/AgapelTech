package com.agapeltech.myapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class SettingsFragment extends Fragment {

    private Button adminLoginBtn, btnManageExpenses, btnSwitchTheme, btnCheckUpdate, btnManageUsers;
    private DBHelper dbHelper;
    private String currentUserRole = "STAFF";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Context ctx = getContext();
        if (ctx == null) ctx = requireContext();

        dbHelper = new DBHelper(ctx);
        currentUserRole = ctx.getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("role", "STAFF");

        initViews(view);
        setupListeners();
        updateThemeButtonUI();
        applyRoleRestrictions();

        return view;
    }

    private void initViews(View v) {
        adminLoginBtn = v.findViewById(R.id.adminLoginBtn);
        btnManageExpenses = v.findViewById(R.id.btnManageExpenses);
        btnSwitchTheme = v.findViewById(R.id.btnSwitchTheme);
        btnCheckUpdate = v.findViewById(R.id.btnCheckUpdate);
        btnManageUsers = v.findViewById(R.id.btnManageUsers);

        adminLoginBtn.setText("LOGOUT");
    }

    private void setupListeners() {
        adminLoginBtn.setOnClickListener(v -> showLogoutDialog());
        btnManageExpenses.setOnClickListener(v -> showExpenseRecordsDialog());
        btnSwitchTheme.setOnClickListener(v -> toggleTheme());
        btnCheckUpdate.setOnClickListener(v -> checkForUpdates());
        btnManageUsers.setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.replaceFragment(new UserManagementFragment(), null);
            }
        });
    }

    private void applyRoleRestrictions() {
        if ("MANAGER".equals(currentUserRole)) {
            btnManageUsers.setVisibility(View.VISIBLE);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    SharedPreferences.Editor editor = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    if (getActivity() != null) getActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleTheme() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !isDarkMode).apply();

        int targetMode = !isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(targetMode);
        updateThemeButtonUI();
    }

    private void updateThemeButtonUI() {
        boolean isDarkMode = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("dark_mode", false);
        btnSwitchTheme.setText(isDarkMode ? "SWITCH TO LIGHT MODE" : "SWITCH TO DARK MODE");
        btnSwitchTheme.setBackgroundColor(isDarkMode ? Color.WHITE : Color.BLACK);
        btnSwitchTheme.setTextColor(isDarkMode ? Color.BLACK : Color.WHITE);
    }

    private void checkForUpdates() {
        String githubApiUrl = "https://api.github.com/repos/innocentmikayi-rgb/AgapelTech/releases/latest";
        final Context appContext = getContext();
        if (appContext == null) return;

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(githubApiUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AgapelTech-App");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
                    String response = scanner.useDelimiter("\\A").next();
                    scanner.close();

                    JSONObject json = new JSONObject(response);
                    String latestVersion = json.getString("tag_name");
                    String downloadUrl = json.getString("html_url");

                    String currentVersion = "v" + appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0).versionName;

                    if (!Objects.equals(latestVersion, currentVersion)) {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> showUpdateDialog(latestVersion, downloadUrl));
                        }
                    } else {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> Toast.makeText(appContext, "You are on the latest version!", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> Toast.makeText(appContext, "Update check failed (Code: " + responseCode + ")", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception e) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> Toast.makeText(appContext, "Update Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void showUpdateDialog(String newVersion, String url) {
        new AlertDialog.Builder(getContext())
                .setTitle("New Update Available!")
                .setMessage("Version " + newVersion + " is now available. Would you like to view it on GitHub?")
                .setPositiveButton("View Update", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showExpenseRecordsDialog() {
        android.widget.ListView eList = new android.widget.ListView(requireContext());
        ArrayList<HashMap<String, String>> eData = new ArrayList<>();
        Cursor c = dbHelper.getAllExpenses();
        if (c != null) {
            while (c.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", "" + c.getInt(0));
                map.put("date", c.getString(1));
                map.put("desc", c.getString(2));
                map.put("amt", MainActivity.formatMoney(c.getDouble(3)));
                map.put("cat", c.getString(4));
                eData.add(map);
            }
            c.close();
        }

        SimpleAdapter eAdapter = new SimpleAdapter(requireContext(), eData, android.R.layout.simple_list_item_2,
                new String[]{"desc", "amt"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                View v = super.getView(pos, convert, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                HashMap<String, String> m = eData.get(pos);
                t1.setText(String.format(Locale.US, "%s - %s [%s]", m.get("date"), m.get("desc"), m.get("cat")));
                t2.setText(String.format(Locale.US, "UGX %s", m.get("amt")));
                t2.setTextColor(Color.RED);
                return v;
            }
        };
        eList.setAdapter(eAdapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle("Expense Records").setView(eList).setPositiveButton("Close", null).create();
        eList.setOnItemClickListener((parent, view, position, id) -> {
            if (!"MANAGER".equals(currentUserRole)) return;
            HashMap<String, String> exp = eData.get(position);
            String[] options = {"Edit Expense", "Delete Expense"};
            new AlertDialog.Builder(requireContext()).setTitle("Manage Expense").setItems(options, (d, which) -> {
                if (which == 0) showEditExpenseDialog(exp, dialog);
                else {
                    String expId = exp.get("id");
                    if (expId != null) dbHelper.markExpenseForDeletion(Integer.parseInt(expId));
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                    dialog.dismiss(); showExpenseRecordsDialog();
                }
            }).show();
        });
        dialog.show();
    }

    private void showEditExpenseDialog(HashMap<String, String> exp, AlertDialog parentDialog) {
        LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ed = new EditText(requireContext()); ed.setText(exp.get("desc")); layout.addView(ed);
        final EditText ea = new EditText(requireContext()); ea.setText(exp.get("amt")); ea.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(ea);
        String[] cats = {"Rent", "Electricity", "Water", "Salary", "Transport", "Stock", "Other"};
        final Spinner sp = new Spinner(requireContext()); sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cats));
        String currentExpCat = exp.get("cat");
        for(int i=0; i<cats.length; i++) if(Objects.equals(cats[i], currentExpCat)) sp.setSelection(i);
        layout.addView(sp);
        
        new AlertDialog.Builder(requireContext()).setTitle("Edit Expense: " + exp.get("date")).setView(layout).setPositiveButton("Update", (d, w) -> {
            String expId = exp.get("id");
            if (expId != null) dbHelper.updateExpenseRecord(Integer.parseInt(expId), ed.getText().toString(), Double.parseDouble(ea.getText().toString()), sp.getSelectedItem().toString());
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
            parentDialog.dismiss(); showExpenseRecordsDialog();
        }).show();
    }
}
