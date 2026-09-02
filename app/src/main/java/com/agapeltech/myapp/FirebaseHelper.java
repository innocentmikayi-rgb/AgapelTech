package com.agapeltech.myapp;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

import androidx.annotation.NonNull;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static final String BASE_URL = "https://myshopapp-3941d-default-rtdb.firebaseio.com";

    // --- SDK Based Methods ---

    public static void getUserRole(String uid, final RoleCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("role");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                if (callback != null) callback.onRoleReceived(role != null ? role : "STAFF");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onRoleReceived("STAFF");
            }
        });
    }

    public static void setUserRole(String uid, String role, final SimpleCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("role");
        ref.setValue(role).addOnCompleteListener(task -> {
            if (callback != null) callback.onComplete(task.isSuccessful());
        });
    }

    // --- Legacy REST Methods (Kept for existing Sync logic) ---

    public static void createRecord(final String path, final JSONObject data, final SaveCallback callback){
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); 
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    reader.close();
                    JSONObject response = new JSONObject(result.toString());
                    if(callback != null) callback.onSaved(response.getString("name"));
                } else {
                    Log.e(TAG, "Create Error: " + code + " for " + path);
                }
            } catch (Exception e) { Log.e(TAG, "Create Exception: " + path, e); }
        }).start();
    }

    public static void saveSale(final JSONObject saleJson, final SaveCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/sales.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); 
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(saleJson.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    reader.close();
                    JSONObject response = new JSONObject(result.toString());
                    if(callback != null) callback.onSaved(response.getString("name"));
                } else {
                    Log.e(TAG, "Save Sale Error: " + code);
                }
            } catch (Exception e) { Log.e(TAG, "Save Sale Exception", e); }
        }).start();
    }

    public static void saveExpense(final JSONObject expJson, final SaveCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/expenses.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); 
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(expJson.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    reader.close();
                    JSONObject response = new JSONObject(result.toString());
                    if(callback != null) callback.onSaved(response.getString("name"));
                } else {
                    Log.e(TAG, "Save Expense Error: " + code);
                }
            } catch (Exception e) { Log.e(TAG, "Save Expense Exception", e); }
        }).start();
    }

    public static void fetchAllData(final String endpoint, final DataCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + endpoint + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    reader.close();
                    if (callback != null) callback.onDataReceived(result.toString());
                }
            } catch (Exception e) { Log.e(TAG, "Fetch Exception: " + endpoint, e); }
        }).start();
    }

    public static void deleteRecord(final String path, final DeleteCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                int code = conn.getResponseCode();
                if ((code == 200 || code == 204) && callback != null) callback.onDeleted();
                else Log.e(TAG, "Delete Error: " + code + " for " + path);
            } catch (Exception e) { Log.e(TAG, "Delete Exception: " + path, e); }
        }).start();
    }

    public static void updateRecord(final String path, final JSONObject data, final UpdateCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); 
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes("UTF-8"));
                os.close();
                
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    if (callback != null) callback.onUpdated();
                } else {
                    Log.e(TAG, "Update Error: " + code + " for " + path);
                }
            } catch (Exception e) { Log.e(TAG, "Update Exception: " + path, e); }
        }).start();
    }

    public interface DataCallback { void onDataReceived(String jsonData); }
    public interface SaveCallback { void onSaved(String key); }
    public interface DeleteCallback { void onDeleted(); }
    public interface UpdateCallback { void onUpdated(); }
    public interface RoleCallback { void onRoleReceived(String role); }
    public interface SimpleCallback { void onComplete(boolean success); }
}
