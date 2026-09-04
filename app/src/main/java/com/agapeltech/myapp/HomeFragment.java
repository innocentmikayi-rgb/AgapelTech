package com.agapeltech.myapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private TextView txtDashNetProfit, txtDashGrossSales, txtDashExpenses, txtDashDebt, txtDashLoans, txtWeeklyGrowth, 
                     txtDashLowStockBadge, txtDashboardGreeting, txtDashboardInventoryCount, txtLowStockItems;
    private TextView txtComparisonTitle, txtComparisonValue, txtGraphTitle;
    private LineChart salesLineChart;
    private PieChart dashProfitPieChart;
    private Button btnQuickExpense, btnQuickDebt, btnQuickLoan;
    private RadioGroup rgGraphRange;
    private View layoutLowStockAlert;

    private DBHelper dbHelper;
    private String currentUsername = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new DBHelper(requireContext());
        
        // Get username from SharedPreferences
        currentUsername = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("username", "");

        initViews(view);
        setupListeners(view);
        loadDashboardData();

        return view;
    }

    private void initViews(View v) {
        txtDashNetProfit = v.findViewById(R.id.txtDashNetProfit);
        txtDashGrossSales = v.findViewById(R.id.txtDashGrossSales);
        txtDashExpenses = v.findViewById(R.id.txtDashExpenses);
        txtDashDebt = v.findViewById(R.id.txtDashDebt);
        txtDashLoans = v.findViewById(R.id.txtDashLoans);
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
        btnQuickLoan = v.findViewById(R.id.btnQuickLoan);
        rgGraphRange = v.findViewById(R.id.rgGraphRange);
        layoutLowStockAlert = v.findViewById(R.id.layoutLowStockAlert);
        txtLowStockItems = v.findViewById(R.id.txtLowStockItems);
    }

    private void setupListeners(View v) {
        btnQuickExpense.setOnClickListener(view -> showExpenseDialog());
        btnQuickDebt.setOnClickListener(view -> showDebtorsDialog());
        btnQuickLoan.setOnClickListener(view -> showLoanDialog());
        
        Button btnAnalytics = v.findViewById(R.id.btnViewAdvancedAnalytics);
        if (btnAnalytics != null) {
            btnAnalytics.setOnClickListener(view -> {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.replaceFragment(new AnalyticsFragment(), null);
                }
            });
        }

        rgGraphRange.setOnCheckedChangeListener((group, checkedId) -> {
            String range = "weekly";
            if (checkedId == R.id.rbMonthly) range = "monthly";
            else if (checkedId == R.id.rbQuarterly) range = "quarterly";
            else if (checkedId == R.id.rbYearly) range = "yearly";
            
            final String finalRange = range;
            new Thread(() -> {
                fetchSalesChartData(finalRange);
            }).start();
        });
    }

    private String formatMoney(double amount) {
        return MainActivity.formatMoney(amount);
    }

    public void loadDashboardData() {
        new Thread(() -> {
            try {
                SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                SimpleDateFormat sdfMonth = new SimpleDateFormat("MM/yyyy", Locale.US);
                String today = sdfDate.format(new Date());
                String currentMonth = sdfMonth.format(new Date());
                
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -1);
                String yesterday = sdfDate.format(cal.getTime());

                // 1. Monthly Totals
                double monthlySalesTotal = 0, monthlyProfitTotal = 0;
                Cursor cSales = dbHelper.getMonthlySalesRecords(currentMonth);
                if (cSales != null) {
                    while (cSales.moveToNext()) { 
                        monthlySalesTotal += cSales.getDouble(8); 
                        monthlyProfitTotal += cSales.getDouble(9); 
                    }
                    cSales.close();
                }
                double monthlyExp = dbHelper.getMonthlyExpenses(currentMonth);
                double monthlyLoans = dbHelper.getMonthlyLoans(currentMonth);
                
                // 2. Inventory Stats
                int totalProducts = 0;
                Cursor totalCur = dbHelper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM materials WHERE synced != -1", null);
                if(totalCur.moveToFirst()) totalProducts = totalCur.getInt(0);
                totalCur.close();

                int lowStockCount = 0;
                Cursor stockCur = dbHelper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM materials WHERE stock_qty <= low_stock_threshold AND synced != -1", null);
                if(stockCur.moveToFirst()) lowStockCount = stockCur.getInt(0);
                stockCur.close();

                // 3. Comparison Stats
                HashMap<String, Double> todayTotals = dbHelper.getDailyTotals(today);
                HashMap<String, Double> yesterdayTotals = dbHelper.getDailyTotals(yesterday);
                
                // 4. Debt Stats
                Cursor cDebt = dbHelper.getCustomersWithDebt();
                double totalDebt = 0;
                if (cDebt != null) { while (cDebt.moveToNext()) totalDebt += cDebt.getDouble(1); cDebt.close(); }

                // 5. Low Stock Details
                StringBuilder lowStockSb = new StringBuilder();
                Cursor cLow = dbHelper.getLowStockItems();
                if (cLow != null) {
                    while (cLow.moveToNext()) {
                        lowStockSb.append("• ").append(cLow.getString(0)).append(" (").append(cLow.getInt(1)).append(" left)\n");
                    }
                    cLow.close();
                }
                final String lowStockDetails = lowStockSb.toString().trim();

                final double fSales = monthlySalesTotal, fProfit = monthlyProfitTotal, fExp = monthlyExp, fDebt = totalDebt, fLoan = monthlyLoans;
                final int fTotalProd = totalProducts, fLowStock = lowStockCount;
                final HashMap<String, Double> fToday = todayTotals, fYest = yesterdayTotals;

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() == null) return;
                        txtDashGrossSales.setText(String.format(Locale.US, "UGX %s", formatMoney(fSales)));
                        txtDashExpenses.setText(String.format(Locale.US, "UGX %s", formatMoney(fExp)));
                        txtDashNetProfit.setText(String.format(Locale.US, "UGX %s", formatMoney(fProfit - fExp)));
                        txtDashboardGreeting.setText(String.format(Locale.US, "Welcome, %s!", currentUsername));
                        txtDashboardInventoryCount.setText(String.format(Locale.US, "%d Products in Inventory", fTotalProd));
                        txtDashLowStockBadge.setText(String.format(Locale.US, "%d Items Low Stock", fLowStock));
                        txtDashLowStockBadge.setTextColor(fLowStock > 0 ? Color.parseColor("#FFD54F") : Color.parseColor("#81C784"));
                        txtDashDebt.setText("UGX " + formatMoney(fDebt));
                        txtDashLoans.setText("UGX " + formatMoney(fLoan));

                        double todaySales = fToday.get("sales") != null ? fToday.get("sales") : 0;
                        double yesterdaySales = fYest.get("sales") != null ? fYest.get("sales") : 0;
                        txtComparisonTitle.setText("vs. Yesterday");
                        if (yesterdaySales > 0) {
                            double growth = ((todaySales - yesterdaySales) / yesterdaySales) * 100;
                            txtComparisonValue.setText(String.format(Locale.US, "%s%.1f%% Sales", growth >= 0 ? "+" : "", growth));
                            txtComparisonValue.setTextColor(growth >= 0 ? Color.parseColor("#2E7D32") : Color.RED);
                        } else {
                            txtComparisonValue.setText("New Day!");
                            txtComparisonValue.setTextColor(Color.GRAY);
                        }
                        
                        // Pie Chart Update
                        updateProfitPieUI(fProfit, fExp);
                        
                        // Low Stock UI
                        if (!lowStockDetails.isEmpty()) {
                            layoutLowStockAlert.setVisibility(View.VISIBLE);
                            txtLowStockItems.setText(lowStockDetails);
                        } else {
                            layoutLowStockAlert.setVisibility(View.GONE);
                        }
                    });
                }
                
                // Initial Chart Data (Weekly)
                fetchSalesChartData("weekly");

            } catch (Exception e) {
                Log.e("HomeFragment", "Error loading dashboard: " + e.getMessage());
            }
        }).start();
    }

    private void fetchSalesChartData(String range) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.US);
        Calendar cal = Calendar.getInstance();

        int days = 7;
        if (Objects.equals(range, "monthly")) days = 30;
        else if (Objects.equals(range, "quarterly")) days = 90;
        else if (Objects.equals(range, "yearly")) days = 365;

        for (int i = days - 1; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(c.getTime());
            HashMap<String, Double> totals = dbHelper.getDailyTotals(dateStr);
            Double salesVal = totals.get("sales");
            entries.add(new Entry(days - 1 - i, salesVal != null ? salesVal.floatValue() : 0f));
            labels.add(sdf.format(c.getTime()));
        }

        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> updateSalesChartUI(range, entries, labels));
        }
    }

    private void updateSalesChartUI(String range, ArrayList<Entry> entries, ArrayList<String> labels) {
        if (salesLineChart == null) return;
        
        txtGraphTitle.setText(String.format(Locale.US, "%s Sales Performance", range.substring(0, 1).toUpperCase() + range.substring(1)));

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

        if (range.equals("weekly") && entries.size() >= 7) {
            txtWeeklyGrowth.setText("Performance calculated for " + range);
        }
    }

    private void updateProfitPieUI(double profit, double expenses) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) profit, "Profit"));
        entries.add(new PieEntry((float) expenses, "Expenses"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#2E7D32"), Color.parseColor("#C62828"));
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
        LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ed = new EditText(requireContext()); ed.setHint("Description"); layout.addView(ed);
        final EditText ea = new EditText(requireContext()); ea.setHint("Amount"); ea.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(ea);
        String[] cats = {"Rent", "Electricity", "Water", "Salary", "Transport", "Stock", "Other"};
        final Spinner sp = new Spinner(requireContext()); sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cats)); layout.addView(sp);
        new AlertDialog.Builder(requireContext()).setTitle("New Expense").setView(layout).setPositiveButton("Save", (dialog, which) -> {
            try {
                String date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                String desc = ed.getText().toString();
                double amount = Double.parseDouble(ea.getText().toString());
                String category = sp.getSelectedItem().toString();

                if (dbHelper.insertExpense(date, desc, amount, category)) {
                    dbHelper.logActivity(currentUsername, "NEW EXPENSE", "Desc: " + desc + ", Amt: " + amount);
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                    Toast.makeText(requireContext(), "Expense Recorded", Toast.LENGTH_SHORT).show(); 
                    loadDashboardData();
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error saving expense: " + e.getMessage());
                Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showLoanDialog() {
        LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText eb = new EditText(requireContext()); eb.setHint("Borrower Name"); layout.addView(eb);
        final EditText ep = new EditText(requireContext()); ep.setHint("Borrower Phone"); ep.setInputType(InputType.TYPE_CLASS_PHONE); layout.addView(ep);
        final EditText ea = new EditText(requireContext()); ea.setHint("Amount"); ea.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(ea);
        final EditText ed = new EditText(requireContext()); ed.setHint("Details/Purpose"); layout.addView(ed);
        
        new AlertDialog.Builder(requireContext()).setTitle("Record New Loan").setView(layout).setPositiveButton("Save", (dialog, which) -> {
            try {
                String date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                String borrower = eb.getText().toString();
                String phone = ep.getText().toString();
                double amount = Double.parseDouble(ea.getText().toString());
                String details = ed.getText().toString();

                if (dbHelper.insertLoan(borrower, phone, amount, date, details)) {
                    dbHelper.logActivity(currentUsername, "NEW LOAN", "Borrower: " + borrower + ", Amt: " + amount);
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                    Toast.makeText(requireContext(), "Loan Recorded", Toast.LENGTH_SHORT).show(); 
                    loadDashboardData();
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error saving loan: " + e.getMessage());
                Toast.makeText(requireContext(), "Failed to save loan", Toast.LENGTH_SHORT).show();
            }
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

        ListView dList = new ListView(getContext());
        SimpleAdapter dAdapter = new SimpleAdapter(getContext(), debtorData, android.R.layout.simple_list_item_2,
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
