package com.agapeltech.myapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.database.Cursor;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.net.ConnectivityManager;
import android.net.Network;
import android.content.Context;
import android.graphics.Color;
import android.widget.AdapterView; 
import android.util.Log;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONObject;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.mlkit.vision.barcode.common.Barcode;

public class MainActivity extends AppCompatActivity {

    private TextView navHomeText, navProductsText, navSalesText, navReportsText, navMoreText;
    private ImageView navHomeIcon, navProductsIcon, navSalesIcon, navReportsIcon, navMoreIcon;
    private LinearLayout navHome, navProducts, navSales, navReports, navMore;
    private LinearLayout layoutHomeContent, layoutProductsContent, layoutSalesContent, layoutReportsContent, layoutMoreContent;
    
    private AutoCompleteTextView saleParticulars;
    private EditText saleQty, saleBP, saleSP, saleActualAmount, saleCustomerName, saleCustomerPhone;
    private TextView txtLiveProfit, txtLiveBalance;
    private Button btnSaveSale;
    private ArrayList<String> inventoryProductList = new ArrayList<>();

    private ListView salesListView;
    private ArrayList<HashMap<String, String>> salesListData = new ArrayList<>();
    private com.agapeltech.myapp.SalesAdapter salesAdapter;

    EditText itemName, buyingPrice, sellingPrice, searchBox, itemQty, lowStockThreshold;
    Spinner itemCategory;
    ImageButton btnScanAdmin, btnScanSale;
    Button saveButton, adminLoginBtn, btnShareReport, btnExportPdf, btnExportExcel, btnManageExpenses, btnSwitchTheme, btnCheckUpdate;
    View adminInputArea, layoutLowStockAlert;
    TextView txtLowStockItems;
    View bottomNavigationContainer;

    TextView txtReportTotalSales, txtReportTotalProfit, txtReportTotalCredit;
    
    TextView txtDashNetProfit, txtDashGrossSales, txtDashExpenses, txtDashDebt, txtWeeklyGrowth;
    TextView txtComparisonTitle, txtComparisonValue, txtGraphTitle;
    LineChart salesLineChart;
    PieChart profitPieChart;
    Button btnQuickExpense, btnQuickDebt;
    RadioGroup rgGraphRange;

    DBHelper dbHelper;
    ListView listView;
    ArrayList<HashMap<String, String>> listData;
    private MaterialAdapter adapter;

    String currentUserRole = "STAFF";
    String currentUsername = "";
    private TextView activeScannerTarget;

    private final ActivityResultLauncher<Intent> scannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String code = result.getData().getStringExtra("SCAN_RESULT");
                    if (code != null && activeScannerTarget != null) {
                        activeScannerTarget.setText(code);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_SHORT).show();
                }
            }
    );

    public static String formatMoney(double amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }

    private boolean isOnline() {
        return NetworkHelper.isOnline(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);

        // Check for session
        android.content.SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        currentUsername = prefs.getString("username", null);
        currentUserRole = prefs.getString("role", null);

        if (currentUsername == null || currentUserRole == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        runOnUiThread(() -> {
                            syncOfflineData();
                            loadFromFirebase();
                        });
                    }
                });
            }
        }

        itemName = findViewById(R.id.itemName);
        btnScanAdmin = findViewById(R.id.btnScanAdmin);
        buyingPrice = findViewById(R.id.buyingPrice);
        sellingPrice = findViewById(R.id.sellingPrice);
        itemQty = findViewById(R.id.itemQty);
        lowStockThreshold = findViewById(R.id.lowStockThreshold);
        itemCategory = findViewById(R.id.itemCategory);
        saveButton = findViewById(R.id.saveButton);
        listView = findViewById(R.id.listView);
        searchBox = findViewById(R.id.searchBox);
        adminLoginBtn = findViewById(R.id.adminLoginBtn);
        adminInputArea = findViewById(R.id.adminInputArea);
        btnSwitchTheme = findViewById(R.id.btnSwitchTheme);
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        layoutLowStockAlert = findViewById(R.id.layoutLowStockAlert);
        txtLowStockItems = findViewById(R.id.txtLowStockItems);
        bottomNavigationContainer = findViewById(R.id.bottom_navigation_bar);
        
        layoutSalesContent = findViewById(R.id.layout_sales_content);
        saleParticulars = findViewById(R.id.saleParticulars);
        btnScanSale = findViewById(R.id.btnScanSale);
        saleQty = findViewById(R.id.saleQty);
        saleBP = findViewById(R.id.saleBP);
        saleSP = findViewById(R.id.saleSP);
        saleActualAmount = findViewById(R.id.saleActualAmount);
        saleCustomerName = findViewById(R.id.saleCustomerName);
        saleCustomerPhone = findViewById(R.id.saleCustomerPhone);
        txtLiveProfit = findViewById(R.id.txtLiveProfit);
        txtLiveBalance = findViewById(R.id.txtLiveBalance);
        btnSaveSale = findViewById(R.id.btnSaveSale);

        layoutReportsContent = findViewById(R.id.layout_reports_content);
        salesListView = findViewById(R.id.salesListView);
        btnShareReport = findViewById(R.id.btnShareReport);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnManageExpenses = findViewById(R.id.btnManageExpenses);
        btnSwitchTheme = findViewById(R.id.btnSwitchTheme);
        txtReportTotalSales = findViewById(R.id.txtReportTotalSales);
        txtReportTotalProfit = findViewById(R.id.txtReportTotalProfit);
        txtReportTotalCredit = findViewById(R.id.txtReportTotalCredit);

        txtDashNetProfit = findViewById(R.id.txtDashNetProfit);
        txtDashGrossSales = findViewById(R.id.txtDashGrossSales);
        txtDashExpenses = findViewById(R.id.txtDashExpenses);
        txtDashDebt = findViewById(R.id.txtDashDebt);
        txtWeeklyGrowth = findViewById(R.id.txtWeeklyGrowth);
        txtComparisonTitle = findViewById(R.id.txtComparisonTitle);
        txtComparisonValue = findViewById(R.id.txtComparisonValue);
        txtGraphTitle = findViewById(R.id.txtGraphTitle);
        rgGraphRange = findViewById(R.id.rgGraphRange);
        salesLineChart = findViewById(R.id.salesLineChart);
        profitPieChart = findViewById(R.id.profitPieChart);
        btnQuickExpense = findViewById(R.id.btnQuickExpense);
        btnQuickDebt = findViewById(R.id.btnQuickDebt);

        navHome = findViewById(R.id.nav_home);
        navProducts = findViewById(R.id.nav_products);
        navSales = findViewById(R.id.nav_sales);
        navReports = findViewById(R.id.nav_reports);
        navMore = findViewById(R.id.nav_more);

        navHomeIcon = (ImageView)((LinearLayout)navHome).getChildAt(0);
        navHomeText = (TextView)((LinearLayout)navHome).getChildAt(1);
        navProductsIcon = (ImageView)((LinearLayout)navProducts).getChildAt(0);
        navProductsText = (TextView)((LinearLayout)navProducts).getChildAt(1);
        navSalesIcon = (ImageView)((LinearLayout)navSales).getChildAt(0);
        navSalesText = (TextView)((LinearLayout)navSales).getChildAt(1);
        navReportsIcon = (ImageView)((LinearLayout)navReports).getChildAt(0);
        navReportsText = (TextView)((LinearLayout)navReports).getChildAt(1);
        navMoreIcon = (ImageView)((LinearLayout)navMore).getChildAt(0);
        navMoreText = (TextView)((LinearLayout)navMore).getChildAt(1);

        layoutHomeContent = findViewById(R.id.layout_home_content);
        layoutProductsContent = findViewById(R.id.layout_products_content);
        layoutMoreContent = findViewById(R.id.layout_more_content);
        btnManageExpenses = findViewById(R.id.btnManageExpenses);

        dbHelper = new DBHelper(this);
        listData = new ArrayList<>();
        adapter = new MaterialAdapter(this, listData, currentUserRole);
        listView.setAdapter(adapter);

        salesAdapter = new com.agapeltech.myapp.SalesAdapter(this, salesListData);
        salesListView.setAdapter(salesAdapter);

        setupBottomNavigation();
        setupSalesLogicEngine();
        setupReportsLogicEngine();
        
        rgGraphRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbWeekly) updateSalesChart("weekly");
            else if (checkedId == R.id.rbMonthly) updateSalesChart("monthly");
            else if (checkedId == R.id.rbQuarterly) updateSalesChart("quarterly");
            else if (checkedId == R.id.rbYearly) updateSalesChart("yearly");
        });

        applyRoleRestrictions();
        setupCategorySpinner();
        loadFromSQLite();

        if (isOnline()) {
            syncOfflineData();
            loadFromFirebase();
        }

        adminLoginBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    android.content.SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        saveButton.setOnClickListener(v -> {
            String name = itemName.getText().toString().trim();
            String buyStr = buyingPrice.getText().toString().trim();
            String sellStr = sellingPrice.getText().toString().trim();
            String qStr = itemQty.getText().toString().trim();
            String tStr = lowStockThreshold.getText().toString().trim();
            String cat = itemCategory.getSelectedItem().toString();
            
            if (name.isEmpty() || buyStr.isEmpty() || sellStr.isEmpty() || qStr.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            int threshold = tStr.isEmpty() ? 5 : Integer.parseInt(tStr);
            dbHelper.insertOrUpdate(name, Double.parseDouble(buyStr), Double.parseDouble(sellStr), Integer.parseInt(qStr), cat, threshold);
            if (isOnline()) syncOfflineData();
            Toast.makeText(this, "Item Saved", Toast.LENGTH_SHORT).show();
            clearInputs(); loadFromSQLite();
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (!"MANAGER".equals(currentUserRole)) { Toast.makeText(this, "Manager access required", Toast.LENGTH_SHORT).show(); return; }
            showOptionsDialog(position);
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterResults(s.toString().trim()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategorySpinner() {
        String[] cats = {"General", "Cement/Building", "Plumbing", "Electrical", "Tools", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        itemCategory.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        highlightTab(navHome);
        navHome.setOnClickListener(v -> highlightTab(navHome));
        navProducts.setOnClickListener(v -> highlightTab(navProducts));
        navSales.setOnClickListener(v -> highlightTab(navSales));
        navReports.setOnClickListener(v -> highlightTab(navReports));
        navMore.setOnClickListener(v -> highlightTab(navMore));
    }

    private void highlightTab(LinearLayout activeTab) {
        LinearLayout[] tabs = {navHome, navProducts, navSales, navReports, navMore};
        for (LinearLayout tab : tabs) {
            if (tab != null) {
                ((ImageView) tab.getChildAt(0)).setColorFilter(Color.parseColor("#555555"));
                ((TextView) tab.getChildAt(1)).setTextColor(Color.parseColor("#555555"));
            }
        }
        if (activeTab != null) {
            ((ImageView) activeTab.getChildAt(0)).setColorFilter(Color.parseColor("#0D3B84"));
            ((TextView) activeTab.getChildAt(1)).setTextColor(Color.parseColor("#0D3B84"));
        }
        if (layoutHomeContent != null) {
            layoutHomeContent.setVisibility(activeTab == navHome ? View.VISIBLE : View.GONE);
            layoutProductsContent.setVisibility(activeTab == navProducts ? View.VISIBLE : View.GONE);
            layoutSalesContent.setVisibility(activeTab == navSales ? View.VISIBLE : View.GONE);
            layoutReportsContent.setVisibility(activeTab == navReports ? View.VISIBLE : View.GONE);
            layoutMoreContent.setVisibility(activeTab == navMore ? View.VISIBLE : View.GONE);
            if (activeTab == navHome) {
                loadDashboardData();
                updateSalesChart("weekly");
            }
            else if (activeTab == navSales) refreshInventoryAutocompleteData();
            else if (activeTab == navReports) {
                loadSalesHistoryFromSQLite();
                updateProfitPieChart();
            }
        }
    }

    private void loadDashboardData() {
        String today = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date());
        String currentMonth = new java.text.SimpleDateFormat("MM/yyyy").format(new Date());
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new java.text.SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());

        double monthlySales = 0, monthlyProfit = 0;
        Cursor cSales = dbHelper.getMonthlySalesRecords("%" + currentMonth);
        if (cSales != null) {
            while (cSales.moveToNext()) { 
                monthlySales += cSales.getDouble(8); 
                monthlyProfit += cSales.getDouble(9); 
            }
            cSales.close();
        }
        
        double monthlyExp = dbHelper.getMonthlyExpenses(currentMonth);
        txtDashGrossSales.setText("UGX " + formatMoney(monthlySales));
        txtDashExpenses.setText("UGX " + formatMoney(monthlyExp));
        txtDashNetProfit.setText("UGX " + formatMoney(monthlyProfit - monthlyExp));
        
        // Comparison Metric: Sales vs Yesterday
        HashMap<String, Double> todayTotals = dbHelper.getDailyTotals(today);
        HashMap<String, Double> yesterdayTotals = dbHelper.getDailyTotals(yesterday);
        double todaySales = todayTotals.get("sales");
        double yesterdaySales = yesterdayTotals.get("sales");
        
        txtComparisonTitle.setText("vs. Yesterday");
        if (yesterdaySales > 0) {
            double growth = ((todaySales - yesterdaySales) / yesterdaySales) * 100;
            txtComparisonValue.setText(String.format("%s%.1f%% Sales", growth >= 0 ? "+" : "", growth));
            txtComparisonValue.setTextColor(growth >= 0 ? Color.parseColor("#2E7D32") : Color.RED);
        } else {
            txtComparisonValue.setText("New Day!");
            txtComparisonValue.setTextColor(Color.GRAY);
        }

        Cursor cDebt = dbHelper.getCustomersWithDebt();
        double totalDebt = 0;
        if (cDebt != null) { while (cDebt.moveToNext()) totalDebt += cDebt.getDouble(1); cDebt.close(); }
        txtDashDebt.setText("UGX " + formatMoney(totalDebt));

        // Low Stock Alert Logic
        Cursor cLow = dbHelper.getLowStockItems();
        if (cLow != null && cLow.getCount() > 0) {
            layoutLowStockAlert.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            while (cLow.moveToNext()) {
                sb.append("• ").append(cLow.getString(0)).append(" (").append(cLow.getInt(1)).append(" left)\n");
            }
            txtLowStockItems.setText(sb.toString().trim());
            cLow.close();
        } else {
            layoutLowStockAlert.setVisibility(View.GONE);
            if (cLow != null) cLow.close();
        }

        updateSalesChart("weekly");
    }

    private void updateSalesChart(String range) {
        if (salesLineChart == null) return;
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        int points = 7;
        java.text.SimpleDateFormat sdfLabel;
        java.text.SimpleDateFormat sdfFull = new java.text.SimpleDateFormat("dd/MM/yyyy");
        String title = "Sales Trends";

        if ("monthly".equals(range)) {
            points = 30;
            cal.add(Calendar.DAY_OF_YEAR, -29);
            sdfLabel = new java.text.SimpleDateFormat("dd/MM");
            txtGraphTitle.setText(R.string.monthly);
        } else if ("quarterly".equals(range)) {
            points = 12; // 12 weeks
            cal.add(Calendar.WEEK_OF_YEAR, -11);
            sdfLabel = new java.text.SimpleDateFormat("'W'w");
            txtGraphTitle.setText(R.string.quarterly);
        } else if ("yearly".equals(range)) {
            points = 12; // 12 months
            cal.add(Calendar.MONTH, -11);
            sdfLabel = new java.text.SimpleDateFormat("MMM");
            txtGraphTitle.setText(R.string.yearly);
        } else { // weekly
            points = 7;
            cal.add(Calendar.DAY_OF_YEAR, -6);
            sdfLabel = new java.text.SimpleDateFormat("dd/MM");
            txtGraphTitle.setText(R.string.weekly);
        }

        double currentTotal = 0;
        
        for (int i = 0; i < points; i++) {
            String label = sdfLabel.format(cal.getTime());
            double sales = 0;
            
            if ("quarterly".equals(range)) {
                // Sum sales for the week
                for (int d = 0; d < 7; d++) {
                    Double s = dbHelper.getDailyTotals(sdfFull.format(cal.getTime())).get("sales");
                    sales += (s != null ? s : 0);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
            } else if ("yearly".equals(range)) {
                // Sum sales for the month
                int currentMonth = cal.get(Calendar.MONTH);
                while (cal.get(Calendar.MONTH) == currentMonth) {
                    Double s = dbHelper.getDailyTotals(sdfFull.format(cal.getTime())).get("sales");
                    sales += (s != null ? s : 0);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
            } else {
                Double s = dbHelper.getDailyTotals(sdfFull.format(cal.getTime())).get("sales");
                sales = (s != null ? s : 0);
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            currentTotal += sales;
            entries.add(new Entry(i, (float) sales));
            labels.add(label);
        }

        LineDataSet dataSet = new LineDataSet(entries, range.toUpperCase() + " Sales");
        
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int color = typedValue.data;

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(android.graphics.Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(40);
        
        LineData lineData = new LineData(dataSet);
        salesLineChart.setData(lineData);
        
        salesLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        salesLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        salesLineChart.getXAxis().setDrawGridLines(false);
        salesLineChart.getXAxis().setGranularity(1f);
        salesLineChart.getXAxis().setTextSize(8f);
        
        salesLineChart.getAxisLeft().setDrawGridLines(true);
        salesLineChart.getAxisRight().setEnabled(false);
        salesLineChart.getDescription().setEnabled(false);
        salesLineChart.animateX(800);
        salesLineChart.invalidate();
    }

    private void updateProfitPieChart() {
        if (profitPieChart == null) return;
        
        HashMap<String, Double> categoryProfit = dbHelper.getCategoryWiseProfit();
        ArrayList<PieEntry> entries = new ArrayList<>();
        
        for (String category : categoryProfit.keySet()) {
            double profit = categoryProfit.get(category);
            if (profit > 0) {
                entries.add(new PieEntry((float) profit, category));
            }
        }
        
        if (entries.isEmpty()) {
            profitPieChart.setVisibility(View.GONE);
            return;
        } else {
            profitPieChart.setVisibility(View.VISIBLE);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        profitPieChart.setData(pieData);
        profitPieChart.getDescription().setEnabled(false);
        profitPieChart.setCenterText("Profit Distribution");
        profitPieChart.setHoleColor(Color.TRANSPARENT);
        
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
        profitPieChart.setCenterTextColor(tv.data);
        
        profitPieChart.animateY(1200);
        profitPieChart.invalidate();
    }

    private void refreshInventoryAutocompleteData() {
        inventoryProductList.clear();
        Cursor cursor = dbHelper.getAllData();
        if (cursor != null) { while (cursor.moveToNext()) inventoryProductList.add(cursor.getString(1)); cursor.close(); }
        saleParticulars.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, inventoryProductList));
    }

    private void setupSalesLogicEngine() {
        saleParticulars.setOnItemClickListener((parent, view, position, id) -> {
            saleBP.setText(String.format("%.0f", dbHelper.getSingleBuyingPrice(parent.getItemAtPosition(position).toString())));
            calculateLiveTotals();
        });
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateLiveTotals(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        saleQty.addTextChangedListener(calculationWatcher); saleBP.addTextChangedListener(calculationWatcher);
        saleSP.addTextChangedListener(calculationWatcher); saleActualAmount.addTextChangedListener(calculationWatcher);
        btnSaveSale.setOnClickListener(v -> saveSalesTransactionRecord());
        btnShareReport.setOnClickListener(v -> shareDailyReport());
        btnExportPdf.setOnClickListener(v -> showPdfOptionsDialog());
        btnExportExcel.setOnClickListener(v -> exportToExcel());
        btnQuickExpense.setOnClickListener(v -> showExpenseDialog());
        btnQuickDebt.setOnClickListener(v -> showDebtorsDialog());
        btnManageExpenses.setOnClickListener(v -> showExpenseRecordsDialog());
        btnSwitchTheme.setOnClickListener(v -> toggleTheme());
        btnCheckUpdate.setOnClickListener(v -> checkForUpdates());
        
        btnScanAdmin.setOnClickListener(v -> startScanner(itemName));
        btnScanSale.setOnClickListener(v -> startScanner(saleParticulars));
    }

    private void startScanner(TextView target) {
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

    private void calculateLiveTotals() {
        try {
            int qty = saleQty.getText().toString().isEmpty() ? 0 : Integer.parseInt(saleQty.getText().toString());
            double bp = saleBP.getText().toString().isEmpty() ? 0 : Double.parseDouble(saleBP.getText().toString());
            double sp = saleSP.getText().toString().isEmpty() ? 0 : Double.parseDouble(saleSP.getText().toString());
            double actualPaid = saleActualAmount.getText().toString().isEmpty() ? 0 : Double.parseDouble(saleActualAmount.getText().toString());
            txtLiveProfit.setText("UGX " + formatMoney((sp - bp) * qty));
            txtLiveBalance.setText("UGX " + formatMoney((qty * sp) - actualPaid));
        } catch (Exception e) {}
    }

    private void saveSalesTransactionRecord() {
        String particular = saleParticulars.getText().toString().trim(), qtyStr = saleQty.getText().toString().trim(), 
               bpStr = saleBP.getText().toString().trim(), spStr = saleSP.getText().toString().trim(), 
               actualPaidStr = saleActualAmount.getText().toString().trim(), 
               customer = saleCustomerName.getText().toString().trim(),
               phone = saleCustomerPhone.getText().toString().trim();
        if (customer.isEmpty()) customer = "Walk-in";
        if (particular.isEmpty() || qtyStr.isEmpty() || spStr.isEmpty()) { Toast.makeText(this, "Fill required fields!", Toast.LENGTH_SHORT).show(); return; }
        int qty = Integer.parseInt(qtyStr); double bp = bpStr.isEmpty() ? 0.0 : Double.parseDouble(bpStr), 
                   sp = Double.parseDouble(spStr), actualPaid = actualPaidStr.isEmpty() ? 0.0 : Double.parseDouble(actualPaidStr);
        double expAmt = qty * sp, expProf = (sp - bp) * qty, bal = expAmt - actualPaid;
        String tag = inventoryProductList.contains(particular) ? "In System Inventory" : "Untracked Item";
        String date = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date());
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        if (dbHelper.insertSaleLog(date, particular, qty, bp, sp, expAmt, expProf, actualPaid, expProf - bal, bal, tag, customer, phone, time)) {
            dbHelper.reduceStock(particular, qty);
            if (isOnline()) syncOfflineData();
            Toast.makeText(this, "Sale Logged", Toast.LENGTH_SHORT).show();
            saleParticulars.setText(""); saleQty.setText(""); saleBP.setText(""); saleSP.setText(""); 
            saleActualAmount.setText(""); saleCustomerName.setText(""); saleCustomerPhone.setText("");
            txtLiveProfit.setText("UGX 0"); txtLiveBalance.setText("UGX 0");
        }
    }

    public void loadSalesHistoryFromSQLite() {
        salesListData.clear();
        String today = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date());
        HashMap<String, Double> totals = dbHelper.getDailyTotals(today);
        double s = totals.get("sales") != null ? totals.get("sales") : 0;
        double p = totals.get("profit") != null ? totals.get("profit") : 0;
        double cDebt = totals.get("credit") != null ? totals.get("credit") : 0;

        txtReportTotalSales.setText("UGX " + formatMoney(s));
        txtReportTotalProfit.setText("UGX " + formatMoney(p));
        txtReportTotalCredit.setText("UGX " + formatMoney(cDebt));
        Cursor cursor = dbHelper.getAllSalesRecords();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(cursor.getInt(0))); map.put("date", cursor.getString(1));
                map.put("particulars", cursor.getString(2)); map.put("qty", String.valueOf(cursor.getInt(3)));
                map.put("sp", formatMoney(cursor.getDouble(5))); 
                map.put("sp_raw", String.valueOf(cursor.getDouble(5)));
                map.put("customer", cursor.getString(12));
                map.put("phone", cursor.getString(13));
                map.put("time", cursor.getString(14));
                map.put("balance", String.valueOf(cursor.getDouble(10)));
                salesListData.add(map);
            }
            cursor.close();
        }
        if (salesAdapter != null) salesAdapter.notifyDataSetChanged();
    }

    private void shareDailyReport() {
        String today = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date());
        HashMap<String, Double> totals = dbHelper.getDailyTotals(today);
        double s = totals.get("sales") != null ? totals.get("sales") : 0;
        double p = totals.get("profit") != null ? totals.get("profit") : 0;
        double cDebt = totals.get("credit") != null ? totals.get("credit") : 0;

        StringBuilder r = new StringBuilder("--- AGAPEL TECH DAILY SALES REPORT ---\nDate: " + today + "\n\nSummary:\nTotal Sales: UGX " + formatMoney(s) + "\nTotal Profit: UGX " + formatMoney(p) + "\nCredit: UGX " + formatMoney(cDebt) + "\n\nTransactions:\n");
        int count = 0;
        for (HashMap<String, String> sale : salesListData) {
            if (sale.get("date").equals(today)) {
                count++;
                r.append(count).append(". ").append(sale.get("particulars")).append(" (").append(sale.get("qty")).append(") - UGX ").append(sale.get("sp")).append("\n");
            }
        }
        Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("text/plain"); intent.putExtra(Intent.EXTRA_TEXT, r.toString());
        startActivity(Intent.createChooser(intent, "Share Report"));
    }

    private void setupReportsLogicEngine() {
        salesListView.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, String> sale = salesListData.get(position);
            String rid = sale.get("id");
            ArrayList<String> opt = new ArrayList<>();
            
            boolean isManager = "MANAGER".equals(currentUserRole);
            
            if (Double.parseDouble(sale.get("balance")) > 0) opt.add("Settle Credit");
            opt.add("Share Receipt");
            
            if (isManager) {
                opt.add("Edit Record");
                opt.add("Delete Record");
            }
            
            new AlertDialog.Builder(this).setTitle("Options").setItems(opt.toArray(new String[0]), (dialog, which) -> {
                String choice = opt.get(which);
                if (choice.equals("Settle Credit")) showSettleDialog(rid);
                else if (choice.equals("Share Receipt")) shareReceipt(sale);
                else if (choice.equals("Edit Record")) showEditSaleDialog(sale);
                else if (choice.equals("Delete Record")) confirmDeleteSale(rid);
            }).show();
        });
    }

    private void shareReceipt(HashMap<String, String> sale) {
        String[] options = {getString(R.string.share_as_text), getString(R.string.share_as_image)};
        new AlertDialog.Builder(this).setTitle(R.string.share_receipt).setItems(options, (dialog, which) -> {
            if (which == 0) generateTextReceipt(sale);
            else generateImageReceipt(sale);
        }).show();
    }

    private void generateTextReceipt(HashMap<String, String> sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(getString(R.string.app_name).toUpperCase()).append(" RECEIPT ---\n");
        sb.append("Date: ").append(sale.get("date")).append(" ").append(sale.get("time") != null ? sale.get("time") : "").append("\n");
        sb.append("Customer: ").append(sale.get("customer") != null ? sale.get("customer") : "Walk-in").append("\n");
        sb.append("---------------------------\n");
        sb.append("Item: ").append(sale.get("particulars")).append("\n");
        sb.append("Qty: ").append(sale.get("qty")).append(" x UGX ").append(sale.get("sp")).append("\n");
        sb.append("---------------------------\n");
        
        double balance = Double.parseDouble(sale.get("balance"));
        String spRawStr = sale.get("sp_raw");
        double spRaw = Double.parseDouble(spRawStr != null ? spRawStr : "0");
        double total = Double.parseDouble(sale.get("qty")) * spRaw;
        
        sb.append("TOTAL: UGX ").append(formatMoney(total)).append("\n");
        if (balance > 0) {
            sb.append("Balance Owed: UGX ").append(formatMoney(balance)).append("\n");
        }
        sb.append("\n").append(getString(R.string.receipt_footer));

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "Share Receipt"));
    }

    private void generateImageReceipt(HashMap<String, String> sale) {
        View receiptView = LayoutInflater.from(this).inflate(R.layout.layout_receipt, null);
        
        TextView date = receiptView.findViewById(R.id.receiptDate);
        TextView customer = receiptView.findViewById(R.id.receiptCustomer);
        TextView particulars = receiptView.findViewById(R.id.receiptParticulars);
        TextView details = receiptView.findViewById(R.id.receiptDetails);
        TextView totalTxt = receiptView.findViewById(R.id.receiptTotal);
        TextView balanceTxt = receiptView.findViewById(R.id.receiptBalance);
        View balanceLayout = receiptView.findViewById(R.id.layoutReceiptBalance);

        String fullDate = sale.get("date");
        if (sale.get("time") != null) fullDate += " " + sale.get("time");
        date.setText(fullDate);
        customer.setText(sale.get("customer") != null ? sale.get("customer") : "Walk-in");
        particulars.setText(sale.get("particulars"));
        details.setText("Qty: " + sale.get("qty") + " x UGX " + sale.get("sp"));
        
        double balance = Double.parseDouble(sale.get("balance"));
        String spRawStr = sale.get("sp_raw");
        double spRaw = Double.parseDouble(spRawStr != null ? spRawStr : "0");
        double total = Double.parseDouble(sale.get("qty")) * spRaw;
        
        totalTxt.setText("UGX " + formatMoney(total));
        if (balance > 0) {
            balanceLayout.setVisibility(View.VISIBLE);
            balanceTxt.setText("UGX " + formatMoney(balance));
        } else {
            balanceLayout.setVisibility(View.GONE);
        }

        // Measure and layout the view
        receiptView.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        receiptView.layout(0, 0, receiptView.getMeasuredWidth(), receiptView.getMeasuredHeight());

        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(receiptView.getMeasuredWidth(),
                receiptView.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        receiptView.draw(canvas);

        File file = new File(getCacheDir(), "Receipt_" + sale.get("id") + ".png");
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Receipt"));
        } catch (IOException e) {
            Toast.makeText(this, "Failed to generate receipt image", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToExcel() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Date,Time,Item,Qty,Buying Price,Selling Price,Expected Amount,Expected Profit,Actual Paid,Actual Profit,Balance,Status,Customer,Phone\n");

        Cursor cursor = dbHelper.getAllSalesRecords();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                csv.append(cursor.getInt(0)).append(",")
                   .append("\"").append(cursor.getString(1)).append("\",")
                   .append("\"").append(cursor.getString(14) != null ? cursor.getString(14) : "").append("\",")
                   .append("\"").append(cursor.getString(2)).append("\",")
                   .append(cursor.getInt(3)).append(",")
                   .append(cursor.getDouble(4)).append(",")
                   .append(cursor.getDouble(5)).append(",")
                   .append(cursor.getDouble(6)).append(",")
                   .append(cursor.getDouble(7)).append(",")
                   .append(cursor.getDouble(8)).append(",")
                   .append(cursor.getDouble(9)).append(",")
                   .append(cursor.getDouble(10)).append(",")
                   .append("\"").append(cursor.getString(11)).append("\",")
                   .append("\"").append(cursor.getString(12)).append("\",")
                   .append("\"").append(cursor.getString(13) != null ? cursor.getString(13) : "").append("\"\n");
            }
            cursor.close();
        }

        File file = new File(getCacheDir(), "Sales_Report_" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv");
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(csv.toString().getBytes());
            out.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.ms-excel");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Excel"));
        } catch (IOException e) {
            Toast.makeText(this, R.string.excel_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettleDialog(String rid) {
        final EditText input = new EditText(this); input.setHint("Amount Paid"); input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this).setTitle("Settle Credit").setView(input).setPositiveButton("Confirm", (dialog, which) -> {
            dbHelper.getWritableDatabase().execSQL("UPDATE sales_table SET actual_amount = actual_amount + " + input.getText().toString() + ", balance = balance - " + input.getText().toString() + ", synced = 0 WHERE id = " + rid);
            if (isOnline()) syncOfflineData();
            loadSalesHistoryFromSQLite();
        }).setNegativeButton("Cancel", null).show();
    }

    private void showEditSaleDialog(HashMap<String, String> sale) {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ep = new EditText(this); ep.setText(sale.get("particulars")); layout.addView(ep);
        final EditText eq = new EditText(this); eq.setText(sale.get("qty")); layout.addView(eq);
        final EditText es = new EditText(this); es.setText(sale.get("sp")); layout.addView(es);
        final EditText epaid = new EditText(this);
        Cursor cur = dbHelper.getReadableDatabase().rawQuery("SELECT actual_amount FROM sales_table WHERE id = ?", new String[]{sale.get("id")});
        if (cur.moveToFirst()) epaid.setText(String.format("%.0f", cur.getDouble(0))); cur.close();
        layout.addView(epaid);
        new AlertDialog.Builder(this).setTitle("Edit Sale").setView(layout).setPositiveButton("Update", (dialog, which) -> {
            try {
                int id = Integer.parseInt(sale.get("id")); String p = ep.getText().toString(); int q = Integer.parseInt(eq.getText().toString()); double s = Double.parseDouble(es.getText().toString()), paid = Double.parseDouble(epaid.getText().toString());
                Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT buying_price FROM sales_table WHERE id = ?", new String[]{String.valueOf(id)});
                if (c.moveToFirst()) {
                    double bp = c.getDouble(0), expAmt = q * s, expProf = (s - bp) * q, bal = expAmt - paid;
                    dbHelper.updateSaleRecord(id, p, q, bp, s, expAmt, expProf, paid, expProf - bal, bal, "Updated");
                    if (isOnline()) syncOfflineData();
                    loadSalesHistoryFromSQLite();
                }
                c.close();
            } catch (Exception e) {}
        }).setNegativeButton("Cancel", null).show();
    }

    private void confirmDeleteSale(String rid) {
        new AlertDialog.Builder(this).setTitle("Delete Record").setMessage("Permanently delete this sale record?")
            .setPositiveButton("Delete", (dialog, which) -> {
                dbHelper.markSaleForDeletion(Integer.parseInt(rid));
                if(isOnline()) syncOfflineData();
                loadSalesHistoryFromSQLite();
            }).setNegativeButton("Cancel", null).show();
    }

    private void showPdfOptionsDialog() {
        String[] options = {"Weekly Report", "Monthly Report"};
        new AlertDialog.Builder(this).setTitle("Generate PDF").setItems(options, (dialog, which) -> generatePdfReport(which == 0 ? "WEEKLY" : "MONTHLY")).show();
    }

    private void generatePdfReport(String type) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas(); Paint paint = new Paint();
        int y = 50; paint.setTextSize(20); paint.setFakeBoldText(true); canvas.drawText("AgapelTech Sales Report", 150, y, paint);
        y += 40; paint.setTextSize(12); paint.setFakeBoldText(false);
        Calendar cal = Calendar.getInstance();
        if (type.equals("WEEKLY")) cal.add(Calendar.DAY_OF_YEAR, -7); else cal.set(Calendar.DAY_OF_MONTH, 1);
        Date start = cal.getTime();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        double total = 0;
        for (HashMap<String, String> sale : salesListData) {
            try {
                if (sdf.parse(sale.get("date")).after(start) || sdf.parse(sale.get("date")).equals(start)) {
                    y += 20; canvas.drawText(sale.get("date") + " - " + sale.get("particulars") + " (x" + sale.get("qty") + ") - UGX " + sale.get("sp"), 50, y, paint);
                    total += Double.parseDouble(sale.get("sp")) * Integer.parseInt(sale.get("qty"));
                }
            } catch (Exception e) {}
        }
        y += 40; paint.setFakeBoldText(true); canvas.drawText("Total Sales: UGX " + String.format("%.0f", total), 50, y, paint);
        document.finishPage(page);
        File file = new File(getCacheDir(), "SalesReport.pdf");
        try {
            if (file.exists()) file.delete();
            document.writeTo(new FileOutputStream(file)); document.close();
            Uri path = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("application/pdf"); intent.putExtra(Intent.EXTRA_STREAM, path); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share PDF"));
        } catch (IOException e) { Toast.makeText(this, "PDF Error", Toast.LENGTH_SHORT).show(); }
    }

    private void filterResults(String query) {
        ArrayList<HashMap<String, String>> filtered = new ArrayList<>();
        String q = query.toLowerCase();
        for (HashMap<String, String> item : listData) {
            if (item.get("name").toLowerCase().contains(q) || item.get("category").toLowerCase().contains(q)) {
                filtered.add(item);
            }
        }
        listView.setAdapter(new MaterialAdapter(this, filtered, currentUserRole));
    }

    private void applyRoleRestrictions() {
        boolean isManager = "MANAGER".equals(currentUserRole);
        
        // Products tab
        adminInputArea.setVisibility(isManager ? View.VISIBLE : View.GONE);
        
        // Sales tab
        saleBP.setVisibility(isManager ? View.VISIBLE : View.GONE);
        if (findViewById(R.id.txtSaleBPLabel) != null) {
            findViewById(R.id.txtSaleBPLabel).setVisibility(isManager ? View.VISIBLE : View.GONE);
        }
        
        // Reports tab
        if (findViewById(R.id.layoutReportProfit) != null) {
            findViewById(R.id.layoutReportProfit).setVisibility(isManager ? View.VISIBLE : View.GONE);
        }
        if (profitPieChart != null) profitPieChart.setVisibility(isManager ? View.VISIBLE : View.GONE);

        // Home Dashboard
        txtDashNetProfit.setVisibility(isManager ? View.VISIBLE : View.GONE);
        txtDashExpenses.setVisibility(isManager ? View.VISIBLE : View.GONE);
        if (findViewById(R.id.dashProfitPieChart) != null) {
            findViewById(R.id.dashProfitPieChart).setVisibility(isManager ? View.VISIBLE : View.GONE);
        }

        // More tab
        adminLoginBtn.setText("LOGOUT (" + currentUsername + ")");
        updateThemeButtonText();
    }

    private void toggleTheme() {
        if (btnSwitchTheme != null) btnSwitchTheme.setEnabled(false);
        
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("dark_mode", !isDarkMode);
        editor.apply();

        // Delay slightly to prevent rapid recreation crashes
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            applyTheme();
            if (btnSwitchTheme != null) btnSwitchTheme.setEnabled(true);
        }, 200);
    }

    private void updateThemeButtonText() {
        if (btnSwitchTheme == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        btnSwitchTheme.setText(isDarkMode ? "SWITCH TO LIGHT MODE" : "SWITCH TO DARK MODE");
        btnSwitchTheme.setBackgroundColor(isDarkMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);
        btnSwitchTheme.setTextColor(isDarkMode ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
    }

    private void applyTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    private void checkForUpdates() {
        // Updated to use your GitHub details
        String githubApiUrl = "https://api.github.com/repos/innocentmikayi/innocentmikayi-rgb/releases/latest";

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(githubApiUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    java.util.Scanner scanner = new java.util.Scanner(url.openStream());
                    String response = scanner.useDelimiter("\\A").next();
                    scanner.close();

                    JSONObject json = new JSONObject(response);
                    String latestVersion = json.getString("tag_name"); // e.g., "v1.0.2"
                    String downloadUrl = json.getString("html_url");

                    String currentVersion = "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

                    if (!latestVersion.equals(currentVersion)) {
                        runOnUiThread(() -> showUpdateDialog(latestVersion, downloadUrl));
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "You are on the latest version!", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Could not check for updates. Please try later.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showUpdateDialog(String newVersion, String url) {
        new AlertDialog.Builder(this)
                .setTitle("New Update Available!")
                .setMessage("Version " + newVersion + " is now available. Would you like to view it on GitHub?")
                .setPositiveButton("View Update", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void clearInputs() { 
        itemName.setText(""); buyingPrice.setText(""); sellingPrice.setText(""); 
        itemQty.setText(""); itemCategory.setSelection(0);
    }

    public void loadFromSQLite() {
        listData.clear();
        Cursor cursor = dbHelper.getAllData(); 
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name", cursor.getString(1)); 
                map.put("buy", "Buy: UGX " + formatMoney(cursor.getDouble(2))); 
                map.put("sell", "Sell: UGX " + formatMoney(cursor.getDouble(3)));
                map.put("qty_raw", String.valueOf(cursor.getInt(6)));
                map.put("threshold", String.valueOf(cursor.getInt(7)));
                map.put("category", cursor.getString(8));
                listData.add(map);
            }
            cursor.close();
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showOptionsDialog(final int position) {
        String[] options = {"Edit Item", "Delete Item"};
        new AlertDialog.Builder(this).setTitle("Manage Item").setItems(options, (dialog, which) -> {
            String name = listData.get(position).get("name");
            if (which == 0) {
                LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40,20,40,20);
                final EditText b = new EditText(this); b.setHint("Buy Price"); 
                final EditText s = new EditText(this); s.setHint("Sell Price");
                final EditText q = new EditText(this); q.setHint("Stock Quantity");
                final EditText t = new EditText(this); t.setHint("Alert Threshold");
                final Spinner cSpin = new Spinner(this);
                String[] cats = {"General", "Cement/Building", "Plumbing", "Electrical", "Tools", "Other"};
                cSpin.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats));

                Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT buying_price, selling_price, stock_qty, category, low_stock_threshold FROM materials WHERE item_name=?", new String[]{name});
                if(c.moveToFirst()) { 
                    b.setText(String.format("%.0f", c.getDouble(0))); 
                    s.setText(String.format("%.0f", c.getDouble(1)));
                    q.setText(String.valueOf(c.getInt(2)));
                    String currentCat = c.getString(3);
                    for(int i=0; i<cats.length; i++) if(cats[i].equals(currentCat)) cSpin.setSelection(i);
                    t.setText(String.valueOf(c.getInt(4)));
                }
                c.close();
                
                layout.addView(b); layout.addView(s); layout.addView(q); layout.addView(t); layout.addView(cSpin);

                new AlertDialog.Builder(this).setTitle("Edit Item: " + name).setView(layout).setPositiveButton("Update", (d, w) -> {
                    dbHelper.insertOrUpdate(name, Double.parseDouble(b.getText().toString()), Double.parseDouble(s.getText().toString()), Integer.parseInt(q.getText().toString()), cSpin.getSelectedItem().toString(), Integer.parseInt(t.getText().toString()));
                    if (isOnline()) syncOfflineData();
                    loadFromSQLite();
                }).show();
            } else { 
                dbHelper.markForDeletion(name); 
                if (isOnline()) syncOfflineData(); 
                loadFromSQLite(); 
            }
        }).show();
    }

    private void showExpenseDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ed = new EditText(this); ed.setHint("Description"); layout.addView(ed);
        final EditText ea = new EditText(this); ea.setHint("Amount"); ea.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(ea);
        String[] cats = {"Rent", "Electricity", "Water", "Salary", "Transport", "Stock", "Other"};
        final Spinner sp = new Spinner(this); sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats)); layout.addView(sp);
        new AlertDialog.Builder(this).setTitle("New Expense").setView(layout).setPositiveButton("Save", (dialog, which) -> {
            try {
                if (dbHelper.insertExpense(new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()), ed.getText().toString(), Double.parseDouble(ea.getText().toString()), sp.getSelectedItem().toString())) {
                    if (isOnline()) syncOfflineData();
                    Toast.makeText(this, "Expense Recorded", Toast.LENGTH_SHORT).show(); loadDashboardData();
                }
            } catch (Exception e) {}
        }).setNegativeButton("Cancel", null).show();
    }

    private void showDebtorsDialog() {
        ArrayList<HashMap<String, String>> debtorData = new ArrayList<>();
        Cursor c = dbHelper.getCustomersWithDebt();
        if (c != null) {
            while (c.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name", c.getString(0));
                map.put("debt", formatMoney(c.getDouble(1)));
                map.put("phone", c.getString(2) != null ? c.getString(2) : "");
                debtorData.add(map);
            }
            c.close();
        }

        if (debtorData.isEmpty()) {
            Toast.makeText(this, "No current debtors.", Toast.LENGTH_SHORT).show();
            return;
        }

        ListView dList = new ListView(this);
        SimpleAdapter dAdapter = new SimpleAdapter(this, debtorData, android.R.layout.simple_list_item_2,
                new String[]{"name", "debt"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                View v = super.getView(pos, convert, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                t1.setText(debtorData.get(pos).get("name"));
                t2.setText("Debt: UGX " + debtorData.get(pos).get("debt"));
                t2.setTextColor(Color.RED);
                return v;
            }
        };
        dList.setAdapter(dAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("List of Debtors").setView(dList).setPositiveButton("Close", null).create();
        dList.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, String> debtor = debtorData.get(position);
            String name = debtor.get("name");
            String phone = debtor.get("phone");
            String debt = debtor.get("debt");

            String[] options = {"Send WhatsApp Reminder", "Call Customer"};
            new AlertDialog.Builder(this).setTitle("Contact " + name).setItems(options, (d, which) -> {
                if (which == 0) {
                    if (phone == null || phone.isEmpty()) Toast.makeText(this, "No phone number saved!", Toast.LENGTH_SHORT).show();
                    else sendWhatsAppReminder(name, phone, debt);
                } else {
                    if (phone == null || phone.isEmpty()) Toast.makeText(this, "No phone number saved!", Toast.LENGTH_SHORT).show();
                    else {
                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        callIntent.setData(Uri.parse("tel:" + phone));
                        startActivity(callIntent);
                    }
                }
            }).show();
        });
        dialog.show();
    }

    private void sendWhatsAppReminder(String name, String phone, String balance) {
        String message = "Hello " + name + ", this is a reminder from AgapelTech regarding your pending balance of UGX " + balance + ". Please settle at your earliest convenience. Thank you!";
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExpenseRecordsDialog() {
        ListView eList = new ListView(this);
        ArrayList<HashMap<String, String>> eData = new ArrayList<>();
        Cursor c = dbHelper.getAllExpenses();
        if (c != null) {
            while (c.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(c.getInt(0)));
                map.put("date", c.getString(1));
                map.put("desc", c.getString(2));
                map.put("amt", formatMoney(c.getDouble(3)));
                map.put("cat", c.getString(4));
                eData.add(map);
            }
            c.close();
        }

        SimpleAdapter eAdapter = new SimpleAdapter(this, eData, android.R.layout.simple_list_item_2,
                new String[]{"desc", "amt"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                View v = super.getView(pos, convert, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                HashMap<String, String> m = eData.get(pos);
                t1.setText(m.get("date") + " - " + m.get("desc") + " [" + m.get("cat") + "]");
                t2.setText("UGX " + m.get("amt"));
                t2.setTextColor(Color.RED);
                return v;
            }
        };
        eList.setAdapter(eAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Expense Records").setView(eList).setPositiveButton("Close", null).create();
        eList.setOnItemClickListener((parent, view, position, id) -> {
            if (!"MANAGER".equals(currentUserRole)) return;
            HashMap<String, String> exp = eData.get(position);
            String[] options = {"Edit Expense", "Delete Expense"};
            new AlertDialog.Builder(this).setTitle("Manage Expense").setItems(options, (d, which) -> {
                if (which == 0) showEditExpenseDialog(exp, dialog);
                else {
                    dbHelper.markExpenseForDeletion(Integer.parseInt(exp.get("id")));
                    if (isOnline()) syncOfflineData();
                    dialog.dismiss(); showExpenseRecordsDialog();
                }
            }).show();
        });
        dialog.show();
    }

    private void showEditExpenseDialog(HashMap<String, String> exp, AlertDialog parentDialog) {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ed = new EditText(this); ed.setText(exp.get("desc")); layout.addView(ed);
        final EditText ea = new EditText(this); ea.setText(exp.get("amt")); ea.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(ea);
        String[] cats = {"Rent", "Electricity", "Water", "Salary", "Transport", "Stock", "Other"};
        final Spinner sp = new Spinner(this); sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats));
        for(int i=0; i<cats.length; i++) if(cats[i].equals(exp.get("cat"))) sp.setSelection(i);
        layout.addView(sp);
        
        new AlertDialog.Builder(this).setTitle("Edit Expense: " + exp.get("date")).setView(layout).setPositiveButton("Update", (d, w) -> {
            dbHelper.updateExpenseRecord(Integer.parseInt(exp.get("id")), ed.getText().toString(), Double.parseDouble(ea.getText().toString()), sp.getSelectedItem().toString());
            if (isOnline()) syncOfflineData();
            parentDialog.dismiss(); showExpenseRecordsDialog(); loadDashboardData();
        }).show();
    }

    private void loadFromFirebase() {
        // 1. Sync Materials
        FirebaseHelper.fetchAllData("/materials", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); Iterator<String> keys = json.keys();
                ArrayList<String> fbKeys = new ArrayList<>();
                while (keys.hasNext()) {
                    String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                    fbKeys.add(key);
                    dbHelper.insertOrUpdate(obj.getString("name"), obj.getDouble("buy"), obj.getDouble("sell"), obj.optInt("qty", 0), obj.optString("cat", "General"), obj.optInt("threshold", 5));
                    dbHelper.updateFirebaseKey(obj.getString("name"), key);
                }
                // Cleanup local records not in Firebase
                Cursor local = dbHelper.getReadableDatabase().rawQuery("SELECT firebase_key, item_name FROM materials WHERE synced = 1", null);
                while(local.moveToNext()) {
                    if (!fbKeys.contains(local.getString(0))) dbHelper.deleteItemPermanently(local.getString(1));
                }
                local.close();
                runOnUiThread(this::loadFromSQLite);
            } catch (Exception e) {}
        });

        // 2. Sync Sales
        FirebaseHelper.fetchAllData("/sales", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); Iterator<String> keys = json.keys();
                ArrayList<String> fbKeys = new ArrayList<>();
                while (keys.hasNext()) {
                    String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                    fbKeys.add(key);
                    dbHelper.upsertSaleFromFirebase(key, 
                        obj.getString("date"), obj.getString("item"), 
                        obj.getInt("qty"), obj.getDouble("bp"), 
                        obj.getDouble("sp"), obj.getDouble("paid"), 
                        obj.optString("customer", "Walk-in"),
                        obj.optString("phone", ""),
                        obj.optString("time", ""));
                }
                // Cleanup
                Cursor local = dbHelper.getReadableDatabase().rawQuery("SELECT firebase_key, id FROM sales_table WHERE synced = 1", null);
                while(local.moveToNext()) {
                    if (!fbKeys.contains(local.getString(0))) dbHelper.deleteSalePermanently(local.getInt(1));
                }
                local.close();
                runOnUiThread(this::loadSalesHistoryFromSQLite);
                runOnUiThread(this::loadDashboardData);
            } catch (Exception e) {}
        });

        // 3. Sync Expenses
        FirebaseHelper.fetchAllData("/expenses", jsonData -> {
            try {
                if (jsonData == null || jsonData.equals("null")) return;
                JSONObject json = new JSONObject(jsonData); Iterator<String> keys = json.keys();
                ArrayList<String> fbKeys = new ArrayList<>();
                while (keys.hasNext()) {
                    String key = keys.next(); JSONObject obj = json.getJSONObject(key);
                    fbKeys.add(key);
                    dbHelper.upsertExpenseFromFirebase(key, 
                        obj.getString("date"), obj.getString("desc"), 
                        obj.getDouble("amount"), obj.getString("cat"));
                }
                // Cleanup
                Cursor local = dbHelper.getReadableDatabase().rawQuery("SELECT firebase_key, id FROM expenses WHERE synced = 1", null);
                while(local.moveToNext()) {
                    if (!fbKeys.contains(local.getString(0))) dbHelper.deleteExpensePermanently(local.getInt(1));
                }
                local.close();
                runOnUiThread(this::loadDashboardData);
            } catch (Exception e) {}
        });
    }

    public void syncOfflineData() {
        if (!isOnline()) return;

        // --- 1. Materials Sync ---
        // Deletions first
        Cursor delCur = dbHelper.getPendingDeletionData();
        while(delCur.moveToNext()) {
            final String name = delCur.getString(1); String key = delCur.getString(4);
            if (key != null) FirebaseHelper.deleteRecord("/materials/" + key, () -> dbHelper.deleteItemPermanently(name));
            else dbHelper.deleteItemPermanently(name);
        }
        delCur.close();

        // Updates/Adds
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
                if (key != null) FirebaseHelper.updateRecord("/materials/" + key, json, () -> runOnUiThread(() -> dbHelper.markAsSynced(id)));
                else FirebaseHelper.createRecord("/materials", json, k -> { dbHelper.updateFirebaseKey(name, k); runOnUiThread(() -> dbHelper.markAsSynced(id)); });
            } catch (Exception e) {}
        }
        if(mCur != null) mCur.close();

        // --- 2. Sales Sync ---
        // Deletions
        Cursor delSale = dbHelper.getPendingDeletionSales();
        while(delSale.moveToNext()) {
            final int id = delSale.getInt(0); String key = delSale.getString(15);
            if (key != null) FirebaseHelper.deleteRecord("/sales/" + key, () -> dbHelper.deleteSalePermanently(id));
            else dbHelper.deleteSalePermanently(id);
        }
        delSale.close();

        // Updates
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
            } catch (Exception e) {}
        }
        if(sCur != null) sCur.close();

        // --- 3. Expenses Sync ---
        // Deletions
        Cursor delExp = dbHelper.getPendingDeletionExpenses();
        while(delExp.moveToNext()) {
            final int id = delExp.getInt(0); String key = delExp.getString(5);
            if (key != null) FirebaseHelper.deleteRecord("/expenses/" + key, () -> dbHelper.deleteExpensePermanently(id));
            else dbHelper.deleteExpensePermanently(id);
        }
        delExp.close();

        // Updates
        Cursor eCur = dbHelper.getUnsyncedExpenses();
        while (eCur != null && eCur.moveToNext()) {
            final int id = eCur.getInt(0); String key = eCur.getString(5);
            try {
                JSONObject eObj = new JSONObject();
                eObj.put("date", eCur.getString(1)); eObj.put("desc", eCur.getString(2));
                eObj.put("amount", eCur.getDouble(3)); eObj.put("cat", eCur.getString(4));
                
                if (key != null) FirebaseHelper.updateRecord("/expenses/" + key, eObj, () -> dbHelper.markExpenseAsSynced(id));
                else FirebaseHelper.saveExpense(eObj, k -> { dbHelper.updateExpenseFirebaseKey(id, k); dbHelper.markExpenseAsSynced(id); });
            } catch (Exception e) {}
        }
        if(eCur != null) eCur.close();
    }
}
