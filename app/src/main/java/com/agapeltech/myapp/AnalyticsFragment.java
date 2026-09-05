package com.agapeltech.myapp;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class AnalyticsFragment extends Fragment {

    private HorizontalBarChart topProductsBarChart;
    private LineChart revenueProfitLineChart;
    private TextView txtAvgTicketSize, txtProfitMargin;
    private View layoutAnalyticsContent;
    private TextView txtNoData;
    private DBHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        dbHelper = new DBHelper(requireContext());
        
        initViews(view);
        
        loadAnalyticsData();

        return view;
    }

    private void initViews(View v) {
        topProductsBarChart = v.findViewById(R.id.topProductsBarChart);
        revenueProfitLineChart = v.findViewById(R.id.revenueProfitLineChart);
        txtAvgTicketSize = v.findViewById(R.id.txtAvgTicketSize);
        txtProfitMargin = v.findViewById(R.id.txtProfitMargin);
        layoutAnalyticsContent = v.findViewById(R.id.layoutAnalyticsContent);
        txtNoData = v.findViewById(R.id.txtNoData);
    }

    private boolean checkIfDataExists() {
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sales_table WHERE synced != -1", null);
        boolean exists = false;
        if (c != null) {
            if (c.moveToFirst()) exists = c.getInt(0) > 0;
            c.close();
        }
        return exists;
    }

    private void showNoDataMessage() {
        if (txtNoData != null) txtNoData.setVisibility(View.VISIBLE);
        // Hide charts but keep header
        for (int i = 0; i < ((ViewGroup)layoutAnalyticsContent).getChildCount(); i++) {
            View child = ((ViewGroup)layoutAnalyticsContent).getChildAt(i);
            if (child.getId() != R.id.txtNoData && child instanceof CardView) {
                child.setVisibility(View.GONE);
            }
        }
    }

    private void loadAnalyticsData() {
        new Thread(() -> {
            try {
                if (!checkIfDataExists()) {
                    if (getActivity() != null) getActivity().runOnUiThread(this::showNoDataMessage);
                    return;
                }

                String currentMonth = new SimpleDateFormat("MM/yyyy", Locale.US).format(new Date());

                // 1. Basic Stats
                double avgTicket = dbHelper.getAverageTransactionValue(currentMonth);
                double totalMonthlyProfit = 0, totalMonthlyRevenue = 0;
                Cursor c = dbHelper.getMonthlySalesRecords(currentMonth);
                if (c != null) {
                    int revIdx = c.getColumnIndex("actual_amount");
                    int profIdx = c.getColumnIndex("actual_profit");
                    while (c.moveToNext()) {
                        if (revIdx != -1) totalMonthlyRevenue += c.getDouble(revIdx);
                        if (profIdx != -1) totalMonthlyProfit += c.getDouble(profIdx);
                    }
                    c.close();
                }

                // 2. Top Products Data
                ArrayList<BarEntry> topProductsEntries = new ArrayList<>();
                ArrayList<String> topProductsLabels = new ArrayList<>();
                Cursor cTop = dbHelper.getTopSellingProducts(5);
                int idx = 0;
                if (cTop != null) {
                    while (cTop.moveToNext()) {
                        topProductsEntries.add(new BarEntry(idx, cTop.getFloat(1)));
                        topProductsLabels.add(cTop.getString(0));
                        idx++;
                    }
                    cTop.close();
                }

                // 3. Trend Chart Data
                ArrayList<Entry> revenueEntries = new ArrayList<>();
                ArrayList<Entry> profitEntries = new ArrayList<>();
                ArrayList<String> trendLabels = new ArrayList<>();
                Calendar cal = Calendar.getInstance();
                for (int i = 29; i >= 0; i--) {
                    Calendar date = (Calendar) cal.clone();
                    date.add(Calendar.DAY_OF_YEAR, -i);
                    String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date.getTime());
                    
                    HashMap<String, Double> totals = dbHelper.getDailyTotals(dateStr);
                    Double salesVal = totals.get("sales");
                    Double profitVal = totals.get("profit");
                    revenueEntries.add(new Entry(29 - i, salesVal != null ? salesVal.floatValue() : 0f));
                    profitEntries.add(new Entry(29 - i, profitVal != null ? profitVal.floatValue() : 0f));
                    trendLabels.add(new SimpleDateFormat("dd/MM", Locale.US).format(date.getTime()));
                }

                final double finalAvg = avgTicket;
                final double finalRev = totalMonthlyRevenue;
                final double finalProf = totalMonthlyProfit;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        txtAvgTicketSize.setText(String.format(Locale.US, "UGX %s", MainActivity.formatMoney(finalAvg)));
                        if (finalRev > 0) {
                            double margin = (finalProf / finalRev) * 100;
                            txtProfitMargin.setText(String.format(Locale.US, "%.1f%%", margin));
                        }
                        
                        setupTopProductsChart(topProductsEntries, topProductsLabels);
                        setupTrendChart(revenueEntries, profitEntries, trendLabels);
                    });
                }
            } catch (Exception e) {
                Log.e("Analytics", "Error loading analytics: " + e.getMessage());
            }
        }).start();
    }

    private void setupTopProductsChart(ArrayList<BarEntry> entries, ArrayList<String> labels) {
        if (entries.isEmpty()) return;
        BarDataSet dataSet = new BarDataSet(entries, "Units Sold");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData data = new BarData(dataSet);
        
        topProductsBarChart.setData(data);
        topProductsBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        topProductsBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        topProductsBarChart.getXAxis().setDrawGridLines(false);
        topProductsBarChart.getAxisLeft().setDrawGridLines(false);
        topProductsBarChart.getDescription().setEnabled(false);
        topProductsBarChart.animateY(1000);
        topProductsBarChart.invalidate();
    }

    private void setupTrendChart(ArrayList<Entry> revenueEntries, ArrayList<Entry> profitEntries, ArrayList<String> labels) {
        if (revenueEntries.isEmpty()) return;
        LineDataSet revSet = new LineDataSet(revenueEntries, "Revenue");
        revSet.setColor(Color.BLUE);
        revSet.setCircleColor(Color.BLUE);

        LineDataSet profSet = new LineDataSet(profitEntries, "Profit");
        profSet.setColor(Color.GREEN);
        profSet.setCircleColor(Color.GREEN);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(revSet);
        dataSets.add(profSet);

        LineData data = new LineData(dataSets);
        revenueProfitLineChart.setData(data);
        revenueProfitLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        revenueProfitLineChart.getXAxis().setLabelCount(5);
        revenueProfitLineChart.getDescription().setEnabled(false);
        revenueProfitLineChart.animateX(1000);
        revenueProfitLineChart.invalidate();
    }

}
