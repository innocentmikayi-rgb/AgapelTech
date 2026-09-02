package com.agapeltech.myapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONObject;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private TextView navHomeText, navProductsText, navSalesText, navReportsText, navMoreText;
    private ImageView navHomeIcon, navProductsIcon, navSalesIcon, navReportsIcon, navMoreIcon;
    private LinearLayout navHome, navProducts, navSales, navReports, navMore;

    private DBHelper dbHelper;
    
    private TextView activeScannerTarget;

    private final ActivityResultLauncher<Intent> scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String code = result.getData().getStringExtra("SCAN_RESULT");
                    if (code != null && activeScannerTarget != null) {
                        activeScannerTarget.setText(code);
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (currentFragment instanceof SalesFragment) {
                            ((SalesFragment) currentFragment).refreshInventoryAutocompleteData(); 
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
        return String.format(java.util.Locale.US, "%,.0f", amount);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Global Crash Handler for better stability
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            android.util.Log.e("CRASH", "Uncaught Exception: ", throwable);
            // Optionally restart app or show a custom error screen
        });

        applyTheme();
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        dbHelper = new DBHelper(this);

        initNavigation();
        setupBottomNavigation();
        
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), navHome);
        }

        setupNetworkCallback();

        if (NetworkHelper.isOnline(this)) {
            syncOfflineData();
            loadFromFirebase();
        }
    }

    private void initNavigation() {
        navHome = findViewById(R.id.nav_home);
        navProducts = findViewById(R.id.nav_products);
        navSales = findViewById(R.id.nav_sales);
        navReports = findViewById(R.id.nav_reports);
        navMore = findViewById(R.id.nav_more);

        navHomeText = findViewById(R.id.nav_home_text);
        navProductsText = findViewById(R.id.nav_products_text);
        navSalesText = findViewById(R.id.nav_sales_text);
        navReportsText = findViewById(R.id.nav_reports_text);
        navMoreText = findViewById(R.id.nav_more_text);

        navHomeIcon = findViewById(R.id.nav_home_icon);
        navProductsIcon = findViewById(R.id.nav_products_icon);
        navSalesIcon = findViewById(R.id.nav_sales_icon);
        navReportsIcon = findViewById(R.id.nav_reports_icon);
        navMoreIcon = findViewById(R.id.nav_more_icon);
        
        // Safety check
        if (navHome == null || navHomeText == null || navHomeIcon == null) {
            android.util.Log.e("MainActivity", "Navigation views not found! Check your layout.");
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> replaceFragment(new HomeFragment(), navHome));
        navProducts.setOnClickListener(v -> replaceFragment(new InventoryFragment(), navProducts));
        navSales.setOnClickListener(v -> replaceFragment(new SalesFragment(), navSales));
        navReports.setOnClickListener(v -> replaceFragment(new ReportsFragment(), navReports));
        navMore.setOnClickListener(v -> replaceFragment(new SettingsFragment(), navMore));
    }

    public void replaceFragment(Fragment fragment, LinearLayout activeTab) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
        highlightTab(activeTab);
    }

    private void highlightTab(LinearLayout activeTab) {
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        navHomeText.setTextColor(inactiveColor); navHomeIcon.setColorFilter(inactiveColor);
        navProductsText.setTextColor(inactiveColor); navProductsIcon.setColorFilter(inactiveColor);
        navSalesText.setTextColor(inactiveColor); navSalesIcon.setColorFilter(inactiveColor);
        navReportsText.setTextColor(inactiveColor); navReportsIcon.setColorFilter(inactiveColor);
        navMoreText.setTextColor(inactiveColor); navMoreIcon.setColorFilter(inactiveColor);

        int activeColor = ContextCompat.getColor(this, R.color.md_primary);
        
        if (activeTab == navHome) { navHomeText.setTextColor(activeColor); navHomeIcon.setColorFilter(activeColor); }
        else if (activeTab == navProducts) { navProductsText.setTextColor(activeColor); navProductsIcon.setColorFilter(activeColor); }
        else if (activeTab == navSales) { navSalesText.setTextColor(activeColor); navSalesIcon.setColorFilter(activeColor); }
        else if (activeTab == navReports) { navReportsText.setTextColor(activeColor); navReportsIcon.setColorFilter(activeColor); }
        else if (activeTab == navMore) { navMoreText.setTextColor(activeColor); navMoreIcon.setColorFilter(activeColor); }
    }

    private void setupNetworkCallback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        runOnUiThread(() -> {
                            syncOfflineData();
                            loadFromFirebase();
                        });
                    }
                });
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
        int targetMode = isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
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
                        if (!isFinishing()) runOnUiThread(() -> dbHelper.markAsSynced(id));
                    });
                    else FirebaseHelper.createRecord("/materials", json, k -> { 
                        dbHelper.updateFirebaseKey(name, k); 
                        if (!isFinishing()) runOnUiThread(() -> dbHelper.markAsSynced(id)); 
                    });
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Sync Error: " + e.getMessage());
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
                    android.util.Log.e("MainActivity", "Sales Sync Error: " + e.getMessage());
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
                    android.util.Log.e("MainActivity", "Expenses Sync Error: " + e.getMessage());
                }
            }
            if(eCur != null) eCur.close();
        }).start();
    }

    public void loadFromFirebase() {
        FirebaseHelper.fetchAllData("/materials", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                    dbHelper.insertOrUpdate(obj.getString("name"), obj.getDouble("buy"), obj.getDouble("sell"), obj.optInt("qty", 0), obj.optString("cat", "General"), obj.optInt("threshold", 5));
                    dbHelper.updateFirebaseKey(obj.getString("name"), key);
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof InventoryFragment) ((InventoryFragment) current).loadFromSQLite();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Materials FB Load Error: " + e.getMessage());
            }
        });

        FirebaseHelper.fetchAllData("/sales", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                    dbHelper.upsertSaleFromFirebase(key, obj.getString("date"), obj.getString("item"), obj.getInt("qty"), obj.getDouble("bp"), obj.getDouble("sp"), obj.getDouble("paid"), obj.optString("customer", "Walk-in"), obj.optString("phone", ""), obj.optString("time", ""));
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof ReportsFragment) ((ReportsFragment) current).loadSalesHistoryFromSQLite();
                        if (current instanceof HomeFragment) ((HomeFragment) current).loadDashboardData();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Sales FB Load Error: " + e.getMessage());
            }
        });

        FirebaseHelper.fetchAllData("/expenses", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                    dbHelper.upsertExpenseFromFirebase(key, obj.getString("date"), obj.getString("desc"), obj.getDouble("amount"), obj.getString("cat"));
                }
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current instanceof HomeFragment) ((HomeFragment) current).loadDashboardData();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Expenses FB Load Error: " + e.getMessage());
            }
        });
    }
}
