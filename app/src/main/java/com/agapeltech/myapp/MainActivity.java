package com.agapeltech.myapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView navHomeText, navProductsText, navSalesText, navReportsText, navChatText, navMoreText;
    private ImageView navHomeIcon, navProductsIcon, navSalesIcon, navReportsIcon, navChatIcon, navMoreIcon;
    private LinearLayout navHome, navProducts, navSales, navReports, navChat, navMore;

    private DBHelper dbHelper;
    private DatabaseReference globalChatRef;
    private ChildEventListener chatListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isChatInitialLoadComplete = false;
    
    private TextView activeScannerTarget;

    private final ActivityResultLauncher<Intent> scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String code = result.getData().getStringExtra("SCAN_RESULT");
                    if (code != null && activeScannerTarget != null) {
                        activeScannerTarget.setText(code);
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (currentFragment instanceof SalesFragment salesFragment) {
                            salesFragment.refreshInventoryAutocompleteData(); 
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) openScanner();
                else Toast.makeText(this, "Camera permission required for scanning", Toast.LENGTH_SHORT).show();
            }
    );

    public static String formatMoney(double amount) {
        return String.format(Locale.US, "%,.0f", amount);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable Edge-to-Edge for Android 15+ compatibility
        EdgeToEdge.enable(this);
        
        // Global Crash Handler for better stability
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH", "Uncaught Exception: ", throwable);
            // Optionally restart app or show a custom error screen
        });

        applyTheme();
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        
        // Handle Insets for Edge-to-Edge compatibility
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation_bar), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return windowInsets;
        });

        dbHelper = new DBHelper(this);

        initNavigation();
        setupBottomNavigation();
        
        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();
        setupGlobalChatListener();

        if (savedInstanceState == null) {
            String openFrag = getIntent().getStringExtra("open_fragment");
            if ("chat".equals(openFrag)) {
                replaceFragment(new ChatFragment(), navChat);
            } else {
                replaceFragment(new HomeFragment(), navHome);
            }
        }

        setupNetworkCallback();

        if (NetworkHelper.isOnline(this)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                syncOfflineData();
                loadFromFirebase();
            }, 1000); // Small delay to prevent UI thread lock on startup
        }
    }

    private void initNavigation() {
        navHome = findViewById(R.id.nav_home);
        navProducts = findViewById(R.id.nav_products);
        navSales = findViewById(R.id.nav_sales);
        navReports = findViewById(R.id.nav_reports);
        navChat = findViewById(R.id.nav_chat);
        navMore = findViewById(R.id.nav_more);

        navHomeText = findViewById(R.id.nav_home_text);
        navProductsText = findViewById(R.id.nav_products_text);
        navSalesText = findViewById(R.id.nav_sales_text);
        navReportsText = findViewById(R.id.nav_reports_text);
        navChatText = findViewById(R.id.nav_chat_text);
        navMoreText = findViewById(R.id.nav_more_text);

        navHomeIcon = findViewById(R.id.nav_home_icon);
        navProductsIcon = findViewById(R.id.nav_products_icon);
        navSalesIcon = findViewById(R.id.nav_sales_icon);
        navReportsIcon = findViewById(R.id.nav_reports_icon);
        navChatIcon = findViewById(R.id.nav_chat_icon);
        navMoreIcon = findViewById(R.id.nav_more_icon);
        
        // Safety check
        if (navHome == null || navHomeText == null || navHomeIcon == null) {
            Log.e("MainActivity", "Navigation views not found! Check your layout.");
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> replaceFragment(new HomeFragment(), navHome));
        navProducts.setOnClickListener(v -> replaceFragment(new InventoryFragment(), navProducts));
        navSales.setOnClickListener(v -> replaceFragment(new SalesFragment(), navSales));
        navReports.setOnClickListener(v -> replaceFragment(new ReportsFragment(), navReports));
        navChat.setOnClickListener(v -> replaceFragment(new ChatFragment(), navChat));
        navMore.setOnClickListener(v -> replaceFragment(new SettingsFragment(), navMore));
    }

    public void replaceFragment(Fragment fragment, LinearLayout activeTab) {
        if (isFinishing() || isDestroyed()) return;
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        
        // If it's a sub-page (no bottom tab mapping), add to backstack
        if (activeTab == null) {
            fragmentTransaction.addToBackStack(null);
        }
        
        fragmentTransaction.commitAllowingStateLoss();
        if (activeTab != null) highlightTab(activeTab);
    }

    private void highlightTab(LinearLayout activeTab) {
        int inactiveColor = ContextCompat.getColor(this, R.color.nav_unselected);
        navHomeText.setTextColor(inactiveColor); navHomeIcon.setColorFilter(inactiveColor);
        navProductsText.setTextColor(inactiveColor); navProductsIcon.setColorFilter(inactiveColor);
        navSalesText.setTextColor(inactiveColor); navSalesIcon.setColorFilter(inactiveColor);
        navReportsText.setTextColor(inactiveColor); navReportsIcon.setColorFilter(inactiveColor);
        navChatText.setTextColor(inactiveColor); navChatIcon.setColorFilter(inactiveColor);
        navMoreText.setTextColor(inactiveColor); navMoreIcon.setColorFilter(inactiveColor);

        int activeColor = ContextCompat.getColor(this, R.color.md_primary);
        
        if (activeTab == navHome) { navHomeText.setTextColor(activeColor); navHomeIcon.setColorFilter(activeColor); }
        else if (activeTab == navProducts) { navProductsText.setTextColor(activeColor); navProductsIcon.setColorFilter(activeColor); }
        else if (activeTab == navSales) { navSalesText.setTextColor(activeColor); navSalesIcon.setColorFilter(activeColor); }
        else if (activeTab == navReports) { navReportsText.setTextColor(activeColor); navReportsIcon.setColorFilter(activeColor); }
        else if (activeTab == navChat) { navChatText.setTextColor(activeColor); navChatIcon.setColorFilter(activeColor); }
        else if (activeTab == navMore) { navMoreText.setTextColor(activeColor); navMoreIcon.setColorFilter(activeColor); }
    }

    private void setupNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                syncOfflineData();
                                loadFromFirebase();
                            }
                        });
                    }
                };
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        }
    }

    public void startScanner(TextView target) {
        this.activeScannerTarget = target;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openScanner() {
        Intent intent = new Intent(this, ScannerActivity.class);
        scannerLauncher.launch(intent);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupGlobalChatListener() {
        globalChatRef = FirebaseDatabase.getInstance().getReference("business_chat");
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isChatInitialLoadComplete) return; // Don't notify for old messages on start
                
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                    String myEmail = prefs.getString("username", "");
                    
                    // Only notify if message is NOT from me
                    if (!message.getSenderEmail().equalsIgnoreCase(myEmail)) {
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (!(currentFragment instanceof ChatFragment)) {
                            NotificationHelper.showChatNotification(MainActivity.this, message.getSenderName(), message.getMessage());
                        }
                    }
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        
        // Use a ValueEventListener once to detect when initial data is loaded
        globalChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Initial data load complete
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                   isChatInitialLoadComplete = true;
                }, 2000);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        globalChatRef.addChildEventListener(chatListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (globalChatRef != null && chatListener != null) {
            globalChatRef.removeEventListener(chatListener);
        }
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        }
    }

    public void refreshInventoryAutocompleteData() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof SalesFragment) {
            ((SalesFragment) currentFragment).refreshInventoryAutocompleteData();
        }
    }

    public boolean isOnline() {
        return NetworkHelper.isOnline(this);
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(targetMode);
    }

    public void syncOfflineData() {
        if (!isOnline()) return;
        new Thread(() -> {
            Cursor delCur = dbHelper.getPendingDeletionData();
            while(delCur.moveToNext()) {
                final String name = delCur.getString(1); String key = delCur.getString(4);
                if (key != null) FirebaseHelper.deleteRecord("/materials/" + key, () -> dbHelper.deleteItemPermanently(name));
                else dbHelper.deleteItemPermanently(name);
            }
            delCur.close();

            Cursor mCur = dbHelper.getUnsyncedData();
            while (mCur != null && mCur.moveToNext()) {
                final int id = mCur.getInt(0); final String name = mCur.getString(1); 
                final double buy = mCur.getDouble(2), sell = mCur.getDouble(3);
                final int qty = mCur.getInt(6); final String cat = mCur.getString(7);
                String key = mCur.getString(4);
                try {
                    JSONObject json = new JSONObject(); 
                    json.put("name", name); json.put("buy", buy); json.put("sell", sell); 
                    json.put("qty", qty); json.put("cat", cat); json.put("threshold", mCur.getInt(7));
                    if (key != null) FirebaseHelper.updateRecord("/materials/" + key, json, () -> {
                        if (!isFinishing()) dbHelper.markAsSynced(id);
                    });
                    else FirebaseHelper.createRecord("/materials", json, k -> { 
                        dbHelper.updateFirebaseKey(name, k); 
                        if (!isFinishing()) dbHelper.markAsSynced(id); 
                    });
                } catch (Exception e) {
                    Log.e("MainActivity", "Sync Error: " + e.getMessage());
                }
            }
            if(mCur != null) mCur.close();

            Cursor delSale = dbHelper.getPendingDeletionSales();
            while(delSale.moveToNext()) {
                final int id = delSale.getInt(0); String key = delSale.getString(15);
                if (key != null) FirebaseHelper.deleteRecord("/sales/" + key, () -> dbHelper.deleteSalePermanently(id));
                else dbHelper.deleteSalePermanently(id);
            }
            delSale.close();

            Cursor sCur = dbHelper.getUnsyncedSales();
            while (sCur != null && sCur.moveToNext()) {
                final int id = sCur.getInt(0); String key = sCur.getString(15);
                try {
                    JSONObject sObj = new JSONObject();
                    sObj.put("date", sCur.getString(1)); sObj.put("item", sCur.getString(2));
                    sObj.put("qty", sCur.getInt(3)); sObj.put("bp", sCur.getDouble(4));
                    sObj.put("sp", sCur.getDouble(5)); sObj.put("paid", sCur.getDouble(8));
                    sObj.put("customer", sCur.getString(12));
                    sObj.put("phone", sCur.getString(13));
                    sObj.put("time", sCur.getString(14));
                    if (key != null) FirebaseHelper.updateRecord("/sales/" + key, sObj, () -> dbHelper.markSaleAsSynced(id));
                    else FirebaseHelper.saveSale(sObj, k -> { dbHelper.updateSaleFirebaseKey(id, k); dbHelper.markSaleAsSynced(id); });
                } catch (Exception e) {
                    Log.e("MainActivity", "Sales Sync Error: " + e.getMessage());
                }
            }
            if(sCur != null) sCur.close();

            Cursor delExp = dbHelper.getPendingDeletionExpenses();
            while(delExp.moveToNext()) {
                final int id = delExp.getInt(0); String key = delExp.getString(5);
                if (key != null) FirebaseHelper.deleteRecord("/expenses/" + key, () -> dbHelper.deleteExpensePermanently(id));
                else dbHelper.deleteExpensePermanently(id);
            }
            delExp.close();

            Cursor eCur = dbHelper.getUnsyncedExpenses();
            while (eCur != null && eCur.moveToNext()) {
                final int id = eCur.getInt(0); String key = eCur.getString(5);
                try {
                    JSONObject eObj = new JSONObject();
                    eObj.put("date", eCur.getString(1)); eObj.put("desc", eCur.getString(2));
                    eObj.put("amount", eCur.getDouble(3)); eObj.put("cat", eCur.getString(4));
                    if (key != null) FirebaseHelper.updateRecord("/expenses/" + key, eObj, () -> dbHelper.markExpenseAsSynced(id));
                    else FirebaseHelper.saveExpense(eObj, k -> { dbHelper.updateExpenseFirebaseKey(id, k); dbHelper.markExpenseAsSynced(id); });
                } catch (Exception e) {
                    Log.e("MainActivity", "Expenses Sync Error: " + e.getMessage());
                }
            }
            if(eCur != null) eCur.close();

            Cursor delLoan = dbHelper.getPendingDeletionLoans();
            while(delLoan.moveToNext()) {
                final int id = delLoan.getInt(0); String key = delLoan.getString(8);
                if (key != null) FirebaseHelper.deleteRecord("/loans/" + key, () -> dbHelper.deleteLoanPermanently(id));
                else dbHelper.deleteLoanPermanently(id);
            }
            delLoan.close();

            Cursor lCur = dbHelper.getUnsyncedLoans();
            while (lCur != null && lCur.moveToNext()) {
                final int id = lCur.getInt(0); String key = lCur.getString(8);
                try {
                    JSONObject lObj = new JSONObject();
                    lObj.put("borrower", lCur.getString(1)); lObj.put("phone", lCur.getString(2));
                    lObj.put("amount", lCur.getDouble(3)); lObj.put("balance", lCur.getDouble(4));
                    lObj.put("date", lCur.getString(5)); lObj.put("details", lCur.getString(6));
                    lObj.put("status", lCur.getString(7));
                    if (key != null) FirebaseHelper.updateRecord("/loans/" + key, lObj, () -> dbHelper.markLoanAsSynced(id));
                    else FirebaseHelper.createRecord("/loans", lObj, k -> { dbHelper.updateLoanFirebaseKey(id, k); dbHelper.markLoanAsSynced(id); });
                } catch (Exception e) {
                    Log.e("MainActivity", "Loans Sync Error: " + e.getMessage());
                }
            }
            if(lCur != null) lCur.close();
        }).start();
    }

    public void loadFromFirebase() {
        FirebaseHelper.fetchAllData("/materials", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); 
                Iterator<String> keys = json.keys();
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    while (keys.hasNext()) {
                        String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                        dbHelper.insertOrUpdate(obj.getString("name"), obj.getDouble("buy"), obj.getDouble("sell"), obj.optInt("qty", 0), obj.optString("cat", "General"), obj.optInt("threshold", 5));
                        dbHelper.updateFirebaseKey(obj.getString("name"), key);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof InventoryFragment) ((InventoryFragment) current).loadFromSQLite();
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Materials FB Load Error: " + e.getMessage());
            }
        });

        FirebaseHelper.fetchAllData("/sales", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); 
                Iterator<String> keys = json.keys();
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    while (keys.hasNext()) {
                        String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                        dbHelper.upsertSaleFromFirebase(key, obj.getString("date"), obj.getString("item"), obj.getInt("qty"), obj.getDouble("bp"), obj.getDouble("sp"), obj.getDouble("paid"), obj.optString("customer", "Walk-in"), obj.optString("phone", ""), obj.optString("time", ""));
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof ReportsFragment) ((ReportsFragment) current).loadSalesHistoryFromSQLite();
                        if (current instanceof HomeFragment) ((HomeFragment) current).loadDashboardData();
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Sales FB Load Error: " + e.getMessage());
            }
        });

        FirebaseHelper.fetchAllData("/expenses", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); 
                Iterator<String> keys = json.keys();
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    while (keys.hasNext()) {
                        String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                        dbHelper.upsertExpenseFromFirebase(key, obj.getString("date"), obj.getString("desc"), obj.getDouble("amount"), obj.getString("cat"));
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof HomeFragment) ((HomeFragment) current).loadDashboardData();
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Expenses FB Load Error: " + e.getMessage());
            }
        });

        FirebaseHelper.fetchAllData("/loans", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); 
                Iterator<String> keys = json.keys();
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    while (keys.hasNext()) {
                        String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                        dbHelper.upsertLoanFromFirebase(key, obj.getString("borrower"), obj.getString("phone"), obj.getDouble("amount"), obj.getDouble("balance"), obj.getString("date"), obj.getString("details"), obj.getString("status"));
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof HomeFragment) ((HomeFragment) current).loadDashboardData();
                        if (current instanceof LoansFragment) ((LoansFragment) current).loadLoansFromSQLite();
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Loans FB Load Error: " + e.getMessage());
            }
        });
    }
}
