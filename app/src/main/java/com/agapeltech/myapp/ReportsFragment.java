package com.agapeltech.myapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class ReportsFragment extends Fragment {

    private Button btnExportPdf, btnExportExcel, btnShareReport;
    private TextView txtReportTotalSales, txtReportTotalProfit, txtReportTotalCredit;
    private PieChart profitPieChart;
    private RecyclerView recyclerViewSales;
    private View headerProfitChart, containerProfitChart;
    private ImageView imgArrowReports;

    private DBHelper dbHelper;
    private ArrayList<HashMap<String, String>> salesListData = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        dbHelper = new DBHelper(requireContext());

        initViews(view);
        setupListeners();
        loadSalesHistoryFromSQLite();

        return view;
    }

    private void initViews(View v) {
        btnExportPdf = v.findViewById(R.id.btnExportPdf);
        btnExportExcel = v.findViewById(R.id.btnExportExcel);
        btnShareReport = v.findViewById(R.id.btnShareReport);
        txtReportTotalSales = v.findViewById(R.id.txtReportTotalSales);
        txtReportTotalProfit = v.findViewById(R.id.txtReportTotalProfit);
        txtReportTotalCredit = v.findViewById(R.id.txtReportTotalCredit);
        profitPieChart = v.findViewById(R.id.profitPieChart);
        recyclerViewSales = v.findViewById(R.id.recyclerViewSales);
        recyclerViewSales.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        headerProfitChart = v.findViewById(R.id.headerProfitChart);
        containerProfitChart = v.findViewById(R.id.containerProfitChart);
        imgArrowReports = v.findViewById(R.id.imgArrowReports);
    }

    private void setupListeners() {
        btnExportPdf.setOnClickListener(v -> showPdfOptionsDialog());
        btnExportExcel.setOnClickListener(v -> exportToExcel());
        btnShareReport.setOnClickListener(v -> shareDailyReport());

        headerProfitChart.setOnClickListener(v -> toggleChartVisibility());
    }

    private void toggleChartVisibility() {
        if (containerProfitChart.getVisibility() == View.VISIBLE) {
            containerProfitChart.setVisibility(View.GONE);
            imgArrowReports.setRotation(0);
        } else {
            containerProfitChart.setVisibility(View.VISIBLE);
            imgArrowReports.setRotation(180);
            updateProfitPieChart();
        }
    }

    public void loadSalesHistoryFromSQLite() {
        salesListData.clear();
        Cursor cursor = dbHelper.getAllSalesRecords();
        double totalSales = 0, totalProfit = 0, totalCredit = 0;
        
        if (cursor != null) {
            int idIdx = cursor.getColumnIndex("id");
            int dateIdx = cursor.getColumnIndex("sale_date");
            int partIdx = cursor.getColumnIndex("particulars");
            int qtyIdx = cursor.getColumnIndex("qty");
            int spIdx = cursor.getColumnIndex("selling_price");
            int paidIdx = cursor.getColumnIndex("actual_amount");
            int balIdx = cursor.getColumnIndex("balance");
            int statusIdx = cursor.getColumnIndex("status_tag");
            int custIdx = cursor.getColumnIndex("customer_name");
            int phoneIdx = cursor.getColumnIndex("customer_phone");
            int timeIdx = cursor.getColumnIndex("sale_time");

            while (cursor.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", idIdx != -1 ? "" + cursor.getInt(idIdx) : "0");
                map.put("date", dateIdx != -1 ? cursor.getString(dateIdx) : "");
                map.put("particulars", partIdx != -1 ? cursor.getString(partIdx) : "");
                map.put("qty", qtyIdx != -1 ? "" + cursor.getInt(qtyIdx) : "0");
                map.put("sp", String.format(Locale.US, "%.0f", spIdx != -1 ? cursor.getDouble(spIdx) : 0));
                map.put("paid", String.format(Locale.US, "%.0f", paidIdx != -1 ? cursor.getDouble(paidIdx) : 0));
                map.put("balance", String.format(Locale.US, "%.0f", balIdx != -1 ? cursor.getDouble(balIdx) : 0));
                map.put("status", statusIdx != -1 ? cursor.getString(statusIdx) : "Unknown");
                map.put("customer", custIdx != -1 ? cursor.getString(custIdx) : "Walk-in");
                map.put("phone", phoneIdx != -1 ? cursor.getString(phoneIdx) : "");
                map.put("time", timeIdx != -1 ? cursor.getString(timeIdx) : "");
                salesListData.add(map);

                if (paidIdx != -1) totalSales += cursor.getDouble(paidIdx);
                if (balIdx != -1) totalCredit += cursor.getDouble(balIdx);
                // Profit calculation fix
                int profIdx = cursor.getColumnIndex("actual_profit");
                if (profIdx != -1) totalProfit += cursor.getDouble(profIdx);
            }
            cursor.close();
        }
        
        txtReportTotalSales.setText(String.format(Locale.US, "UGX %s", formatMoney(totalSales)));
        txtReportTotalProfit.setText(String.format(Locale.US, "UGX %s", formatMoney(totalProfit)));
        txtReportTotalCredit.setText(String.format(Locale.US, "UGX %s", formatMoney(totalCredit)));

        SalesRecyclerViewAdapter salesAdapter = new SalesRecyclerViewAdapter(requireContext(), salesListData, position -> {
            HashMap<String, String> sale = salesListData.get(position);
            String[] options = {"Share Receipt", "Settle Credit", "Edit Record", "Delete Record"};
            new AlertDialog.Builder(requireContext()).setTitle("Manage Sale").setItems(options, (dialog, which) -> {
                if (which == 0) shareReceipt(sale);
                else if (which == 1) showSettleDialog(sale.get("id"));
                else if (which == 2) showEditSaleDialog(sale);
                else if (which == 3) confirmDeleteSale(sale.get("id"));
            }).show();
        });
        recyclerViewSales.setAdapter(salesAdapter);
        
        updateProfitPieChart();
    }

    private void updateProfitPieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        HashMap<String, Double> categoryProfit = dbHelper.getCategoryWiseProfit();
        
        for (String category : categoryProfit.keySet()) {
            Double profitVal = categoryProfit.get(category);
            float profit = profitVal != null ? profitVal.floatValue() : 0f;
            if (profit > 0) {
                entries.add(new PieEntry(profit, category));
            }
        }

        if (entries.isEmpty()) entries.add(new PieEntry(1f, "No Sales"));

        PieDataSet dataSet = new PieDataSet(entries, "Profit by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        profitPieChart.setData(data);
        profitPieChart.getDescription().setEnabled(false);
        profitPieChart.animateY(1000);
        profitPieChart.invalidate();
    }

    private void shareDailyReport() {
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
        HashMap<String, Double> totals = dbHelper.getDailyTotals(today);
        
        Double salesVal = totals.get("sales");
        Double profitVal = totals.get("profit");
        Double debtVal = totals.get("debt");
        
        String report = "AgapelTech Daily Report (" + today + ")\n\n" +
                "Gross Sales: UGX " + formatMoney(salesVal != null ? salesVal : 0) + "\n" +
                "Net Profit: UGX " + formatMoney(profitVal != null ? profitVal : 0) + "\n" +
                "New Debt: UGX " + formatMoney(debtVal != null ? debtVal : 0) + "\n\n" +
                "Keep growing!";
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(intent, "Share Report"));
    }

    private void shareReceipt(HashMap<String, String> sale) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 500, 1).create(); // Thermal receipt size roughly
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        int y = 40;
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("AGAPEL TECH", 80, y, paint);
        
        y += 20;
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        canvas.drawText("Retail Management System", 85, y, paint);
        
        y += 30;
        paint.setStrokeWidth(1);
        canvas.drawLine(20, y, 280, y, paint);
        
        y += 25;
        paint.setFakeBoldText(true);
        canvas.drawText("OFFICIAL RECEIPT", 100, y, paint);
        
        y += 25;
        paint.setFakeBoldText(false);
        canvas.drawText("Date: " + sale.get("date") + " " + sale.get("time"), 20, y, paint);
        canvas.drawText("Receipt ID: #" + sale.get("id"), 20, y + 15, paint);
        
        y += 45;
        paint.setFakeBoldText(true);
        canvas.drawText("ITEM", 20, y, paint);
        canvas.drawText("QTY", 180, y, paint);
        canvas.drawText("TOTAL", 230, y, paint);
        
        y += 10;
        canvas.drawLine(20, y, 280, y, paint);
        
        y += 20;
        paint.setFakeBoldText(false);
        String itemName = sale.get("particulars");
        if (itemName != null && itemName.length() > 20) itemName = itemName.substring(0, 17) + "...";
        String qty = sale.get("qty");
        String paid = sale.get("paid");
        canvas.drawText(itemName != null ? itemName : "", 20, y, paint);
        canvas.drawText(qty != null ? qty : "0", 185, y, paint);
        canvas.drawText(paid != null ? paid : "0", 230, y, paint);
        
        y += 40;
        canvas.drawLine(20, y, 280, y, paint);
        
        y += 20;
        paint.setFakeBoldText(true);
        canvas.drawText("TOTAL PAID:", 20, y, paint);
        canvas.drawText("UGX " + sale.get("paid"), 180, y, paint);
        
        y += 15;
        canvas.drawText("BALANCE:", 20, y, paint);
        canvas.drawText("UGX " + sale.get("balance"), 180, y, paint);
        
        y += 40;
        paint.setTextSize(9);
        paint.setFakeBoldText(false);
        canvas.drawText("Customer: " + sale.get("customer"), 20, y, paint);
        
        y += 30;
        paint.setTextSize(10);
        paint.setFakeBoldText(true);
        canvas.drawText("THANK YOU FOR YOUR BUSINESS!", 60, y, paint);
        
        document.finishPage(page);
        
        File file = new File(requireContext().getCacheDir(), "Receipt_" + sale.get("id") + ".pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            
            Uri path = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, path);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Receipt"));
        } catch (IOException e) {
            Toast.makeText(requireContext(), "PDF Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToExcel() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Date,Item,Qty,Price,Paid,Balance,Status\n");
        for (HashMap<String, String> sale : salesListData) {
            csv.append(sale.get("id")).append(",")
               .append(sale.get("date")).append(",")
               .append(sale.get("particulars")).append(",")
               .append(sale.get("qty")).append(",")
               .append(sale.get("sp")).append(",")
               .append(sale.get("paid")).append(",")
               .append(sale.get("balance")).append(",")
               .append(sale.get("status")).append("\n");
        }

        File file = new File(requireContext().getCacheDir(), "Sales_Report_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + ".csv");
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(csv.toString().getBytes());
            out.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.ms-excel");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Excel"));
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettleDialog(String rid) {
        final EditText input = new EditText(requireContext()); input.setHint("Amount Paid"); input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(requireContext()).setTitle("Settle Credit").setView(input).setPositiveButton("Confirm", (dialog, which) -> {
            dbHelper.getWritableDatabase().execSQL("UPDATE sales_table SET actual_amount = actual_amount + " + input.getText().toString() + ", balance = balance - " + input.getText().toString() + ", synced = 0 WHERE id = " + rid);
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
            loadSalesHistoryFromSQLite();
        }).setNegativeButton("Cancel", null).show();
    }

    private void showEditSaleDialog(HashMap<String, String> sale) {
        LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 40, 50, 10);
        final EditText ep = new EditText(requireContext()); ep.setText(sale.get("particulars")); layout.addView(ep);
        final EditText eq = new EditText(requireContext()); eq.setText(sale.get("qty")); layout.addView(eq);
        final EditText es = new EditText(requireContext()); es.setText(sale.get("sp")); layout.addView(es);
        final EditText epaid = new EditText(requireContext());
        String saleId = sale.get("id");
        if (saleId == null) return;
        Cursor cur = dbHelper.getReadableDatabase().rawQuery("SELECT actual_amount FROM sales_table WHERE id = ?", new String[]{saleId});
        if (cur.moveToFirst()) epaid.setText(String.format(Locale.US, "%.0f", cur.getDouble(0))); cur.close();
        layout.addView(epaid);
        new AlertDialog.Builder(requireContext()).setTitle("Edit Sale").setView(layout).setPositiveButton("Update", (dialog, which) -> {
            try {
                if (saleId == null) return;
                int id = Integer.parseInt(saleId); String p = ep.getText().toString(); int q = Integer.parseInt(eq.getText().toString()); double s = Double.parseDouble(es.getText().toString()), paid = Double.parseDouble(epaid.getText().toString());
                Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT buying_price FROM sales_table WHERE id = ?", new String[]{"" + id});
                if (c.moveToFirst()) {
                    double bp = c.getDouble(0), expAmt = q * s, expProf = (s - bp) * q, bal = expAmt - paid;
                    dbHelper.updateSaleRecord(id, p, q, bp, s, expAmt, expProf, paid, expProf - bal, bal, "Updated");
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                    loadSalesHistoryFromSQLite();
                }
                c.close();
            } catch (Exception e) {
                android.util.Log.e("ReportsFragment", "Error updating sale: " + e.getMessage());
                Toast.makeText(requireContext(), "Error updating sale", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void confirmDeleteSale(String rid) {
        new AlertDialog.Builder(requireContext()).setTitle("Delete Record").setMessage("Permanently delete this sale record?")
            .setPositiveButton("Delete", (dialog, which) -> {
                dbHelper.markSaleForDeletion(Integer.parseInt(rid));
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                loadSalesHistoryFromSQLite();
            }).setNegativeButton("Cancel", null).show();
    }

    private void showPdfOptionsDialog() {
        String[] options = {"Weekly Report", "Monthly Report"};
        new AlertDialog.Builder(requireContext()).setTitle("Generate PDF").setItems(options, (dialog, which) -> generatePdfReport(which == 0 ? "WEEKLY" : "MONTHLY")).show();
    }

    private void generatePdfReport(String type) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas(); Paint paint = new Paint();
        int y = 50; paint.setTextSize(20); paint.setFakeBoldText(true); canvas.drawText("AgapelTech Sales Report", 150, y, paint);
        y += 40; paint.setTextSize(12); paint.setFakeBoldText(false);
        Calendar cal = Calendar.getInstance();
        if (Objects.equals(type, "WEEKLY")) cal.add(Calendar.DAY_OF_YEAR, -7); else cal.set(Calendar.DAY_OF_MONTH, 1);
        Date start = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        double total = 0;
        for (HashMap<String, String> sale : salesListData) {
            try {
                String saleDate = sale.get("date");
                if (saleDate != null) {
                    Date date = sdf.parse(saleDate);
                    if (date != null && (date.after(start) || date.equals(start))) {
                        String particulars = sale.get("particulars");
                        String qty = sale.get("qty");
                        String sp = sale.get("sp");
                        y += 20; canvas.drawText(saleDate + " - " + particulars + " (x" + qty + ") - UGX " + sp, 50, y, paint);
                        if (sp != null && qty != null) total += Double.parseDouble(sp) * Integer.parseInt(qty);
                    }
                }
            } catch (Exception e) {}
            if (y > 800) break; // Simple overflow check
        }
        y += 40; paint.setFakeBoldText(true); canvas.drawText(String.format(Locale.US, "Total Sales: UGX %s", formatMoney(total)), 50, y, paint);
        document.finishPage(page);
        File file = new File(requireContext().getCacheDir(), "SalesReport.pdf");
        try {
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) android.util.Log.w("ReportsFragment", "Could not delete old PDF");
            }
            document.writeTo(new FileOutputStream(file)); document.close();
            Uri path = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("application/pdf"); intent.putExtra(Intent.EXTRA_STREAM, path); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share PDF"));
        } catch (IOException e) { 
            android.util.Log.e("ReportsFragment", "PDF Generation Error: " + e.getMessage());
            Toast.makeText(requireContext(), "PDF Error", Toast.LENGTH_SHORT).show(); 
        }
    }

    private String formatMoney(double amount) {
        return MainActivity.formatMoney(amount);
    }
}
