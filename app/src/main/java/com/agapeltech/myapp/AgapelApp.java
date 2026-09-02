package com.agapeltech.myapp;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class AgapelApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase Offline Persistence
        // This allows the app to remember cloud data even after a full restart without internet
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
