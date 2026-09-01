package com.agapeltech.myapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class HomeFragment extends Fragment {

    private TextView txtDashNetProfit, txtDashGrossSales, txtDashExpenses, txtDashDebt, txtWeeklyGrowth, 
                     txtDashLowStockBadge, txtDashboardGreeting, txtDashboardInventoryCount, txtLowStockItems;
    private TextView txtComparisonTitle, txtComparisonValue, txtGraphTitle;
    private LineChart salesLineChart;
    private PieChart dashProfitPieChart;
    private Button btnQuickExpense, btnQuickDebt;
    private RadioGroup rgGraphRange;
    private View layoutLowStockAlert;

    private DBHelper dbHelper;
    private String currentUsername = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new DBHelper(getContext());
        
        // Get username from SharedPreferences
        currentUsername = getContext().getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("username", "");

        initViews(view);
        setupListeners();
        loadDashboardData();

        return view;
    }

    private void initViews(View v) {
        txtDashNetProfit = v.findViewById(R.id.txtDashNetProfit);
        txtDashGrossSales = v.findViewById(R.id.txtDashGrossSales);
        txtDashExpenses = v.findViewById(R.id.txtDashExpenses);
        txtDashDebt = v.findViewById(R.id.txtDashDebt);
        txtDashLowStockBadge = v.findViewById(R.id.txtDashLowStockBadge);
        txtDashboardGreeting = v.findViewById(R.id.txtDashboardGreeting);
        txtDashboardInventoryCount = v.findViewById(R.id.txtDashboardInventoryCount);
        txtWeeklyGrowth = v.findViewById(R.id.txtWeeklyGrowth);
        txtComparisonTitle = v.findViewById(R.id.txtComparisonTitle);
        txtComparisonValue = v.findViewById(R.id.txtComparisonValue);
        txtGraphTitle = v.findViewById(R.id.txtGraphTitle);
        salesLineChart = v.findViewById(R.id.salesLineChart);
        dashProfitPieChart = v.findViewById(R.id.dashProfitPieChart);
        btnQuickExpense = v.findViewById(R.id.btnQuickExpense);
        btnQuickDebt = v.findViewById(R.id.btnQuickDebt);
        rgGraphRange = v.findViewById(R.id.rgGraphRange);
        layoutLowStockAlert = v.findViewById(R.id.layoutLowStockAlert);
        txtLowStockItems = v.findViewById(R.id.txtLowStockItems);
    }

    private void setupListeners() {
        btnQuickExpense.setOnClickListener(v -> showExpenseDialog());
        btnQuickDebt.setOnClickListener(v -> showDebtorsDialog());
        rgGraphRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbWeekly) updateSalesChart("weekly");
            else if (checkedId == R.id.rbMonthly) updateSalesChart("monthly");
            else if (checkedId == R.id.rbQuarterly) updateSalesChart("quarterly");
            else if (checkedId == R.id.rbYearly) updateSalesChart("yearly");
        });
    }

    private String formatMoney(double amount) {
        return MainActivity.formatMoney(amount);
    }

    public void loadDashboardData() {
        String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        String currentMonth = new SimpleDateFormat("MM/yyyy").format(new Date());
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());

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
        
        // v1.0.3 New Features
        txtDashboardGreeting.setText("Welcome, " + currentUsername + "!");
        
        int totalProducts = 0;
        Cursor totalCur = dbHelper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM materials", null);
        if(totalCur.moveToFirst()) totalProducts = totalCur.getInt(0);
        totalCur.close();
        txtDashboardInventoryCount.setText(totalProducts + " Products in Inventory");

        // Low Stock Badge
        int lowStockCount = 0;
        Cursor stockCur = dbHelper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM materials WHERE stock_qty <= low_stock_threshold", null);
        if(stockCur.moveToFirst()) lowStockCount = stockCur.getInt(0);
        stockCur.close();
        txtDashLowStockBadge.setText(lowStockCount + " Items Low Stock");
        txtDashLowStockBadge.setTextColor(lowStockCount > 0 ? Color.parseColor("#FFD54F") : Color.parseColor("#81C784"));

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
        updateProfitPieChart();
    }

    private void updateSalesChart(String range) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        Calendar cal = Calendar.getInstance();

        int days = 7;
        if (range.equals("monthly")) days = 30;
        else if (range.equals("quarterly")) days = 90;
        else if (range.equals("yearly")) days = 365;

        txtGraphTitle.setText(range.substring(0, 1).toUpperCase() + range.substring(1) + " Sales Performance");

        for (int i = days - 1; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(c.getTime());
            HashMap<String, Double> totals = dbHelper.getDailyTotals(dateStr);
            entries.add(new Entry(days - 1 - i, totals.get("sales").floatValue()));
            labels.add(sdf.format(c.getTime()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Gross Sales (UGX)");
        dataSet.setColor(Color.parseColor("#0D3B84"));
        dataSet.setCircleColor(Color.parseColor("#0D3B84"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E9EFF8"));

        LineData lineData = new LineData(dataSet);
        salesLineChart.setData(lineData);
        salesLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        salesLineChart.getXAxis().setLabelCount(5);
        salesLineChart.getDescription().setEnabled(false);
        salesLineChart.animateX(1000);
        salesLineChart.invalidate();

        if (range.equals("weekly")) {
            float last7 = 0, prev7 = 0;
            for (int i = 0; i < 7; i++) last7 += entries.get(entries.size() - 1 - i).getY();
            // This is simplified, just showing the badge logic
            txtWeeklyGrowth.setText("Performance calculated for " + range);
        }
    }

    private void updateProfitPieChart() {
        String currentMonth = new SimpleDateFormat("MM/yyyy").format(new Date());
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

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) monthlyProfit, "Profit"));
        entries.add(new PieEntry((float) monthlyExp, "Expenses"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#2E7D32"), Color.parseColor("#C62828")});
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        dashProfitPieChart.setData(data);
        dashProfitPieChart.setCenterText("Monthly Split");
        dashProfitPieChart.getDescription().setEnabled(false);
        dashProfitPieChart.animateY(1000);
        dashProfitPieChart.invalidate();
    }

    private void showExpenseDialog() {
        LinearLayout layout = new LinearLayout(getContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ed = new EditText(getContext()); ed.setHint("Description"); layout.addView(ed);
        final EditText ea = new EditText(getContext()); ea.setHint("Amount"); ea.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(ea);
        String[] cats = {"Rent", "Electricity", "Water", "Salary", "Transport", "Stock", "Other"};
        final Spinner sp = new Spinner(getContext()); sp.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, cats)); layout.addView(sp);
        new AlertDialog.Builder(getContext()).setTitle("New Expense").setView(layout).setPositiveButton("Save", (dialog, which) -> {
            try {
                if (dbHelper.insertExpense(new SimpleDateFormat("dd/MM/yyyy").format(new Date()), ed.getText().toString(), Double.parseDouble(ea.getText().toString()), sp.getSelectedItem().toString())) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(getContext())) activity.syncOfflineData();
                    Toast.makeText(getContext(), "Expense Recorded", Toast.LENGTH_SHORT).show(); 
                    loadDashboardData();
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
            Toast.makeText(getContext(), "No current debtors.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.ListView dList = new android.widget.ListView(getContext());
        android.widget.SimpleAdapter dAdapter = new android.widget.SimpleAdapter(getContext(), debtorData, android.R.layout.simple_list_item_2,
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

        AlertDialog dialog = new AlertDialog.Builder(getContext()).setTitle("List of Debtors").setView(dList).setPositiveButton("Close", null).create();
        dList.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, String> debtor = debtorData.get(position);
            String name = debtor.get("name");
            String phone = debtor.get("phone");
            String debt = debtor.get("debt");

            String[] options = {"Send WhatsApp Reminder", "Call Customer"};
            new AlertDialog.Builder(getContext()).setTitle("Contact " + name).setItems(options, (d, which) -> {
                if (which == 0) {
                    if (phone == null || phone.isEmpty()) Toast.makeText(getContext(), "No phone number saved!", Toast.LENGTH_SHORT).show();
                    else sendWhatsAppReminder(name, phone, debt);
                } else {
                    if (phone == null || phone.isEmpty()) Toast.makeText(getContext(), "No phone number saved!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "WhatsApp not installed!", Toast.LENGTH_SHORT).show();
        }
    }
}
