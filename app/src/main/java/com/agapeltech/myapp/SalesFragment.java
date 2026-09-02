package com.agapeltech.myapp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SalesFragment extends Fragment {

    private AutoCompleteTextView saleParticulars;
    private EditText saleQty, saleBP, saleSP, saleActualAmount, saleCustomerName, saleCustomerPhone;
    private TextView txtLiveProfit, txtLiveBalance;
    private Button btnSaveSale;
    private ImageButton btnScanSale;

    private DBHelper dbHelper;
    private ArrayList<String> inventoryProductList = new ArrayList<>();
    private String currentUserRole = "STAFF";
    private String currentUsername = "Unknown";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        dbHelper = new DBHelper(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("role", "STAFF");
        currentUsername = prefs.getString("username", "Unknown");

        initViews(view);
        setupSalesLogicEngine();
        refreshInventoryAutocompleteData();

        return view;
    }

    private void initViews(View v) {
        saleParticulars = v.findViewById(R.id.saleParticulars);
        saleQty = v.findViewById(R.id.saleQty);
        saleBP = v.findViewById(R.id.saleBP);
        saleSP = v.findViewById(R.id.saleSP);
        saleActualAmount = v.findViewById(R.id.saleActualAmount);
        saleCustomerName = v.findViewById(R.id.saleCustomerName);
        saleCustomerPhone = v.findViewById(R.id.saleCustomerPhone);
        txtLiveProfit = v.findViewById(R.id.txtLiveProfit);
        txtLiveBalance = v.findViewById(R.id.txtLiveBalance);
        btnSaveSale = v.findViewById(R.id.btnSaveSale);
        btnScanSale = v.findViewById(R.id.btnScanSale);

        applyRoleRestrictions();
    }

    private void applyRoleRestrictions() {
        boolean isManager = "MANAGER".equals(currentUserRole);
        saleBP.setVisibility(isManager ? View.VISIBLE : View.GONE);
        View bpLabel = getView() != null ? getView().findViewById(R.id.txtSaleBPLabel) : null;
        if (bpLabel != null) bpLabel.setVisibility(isManager ? View.VISIBLE : View.GONE);
    }

    private void setupSalesLogicEngine() {
        saleParticulars.setOnItemClickListener((parent, view, position, id) -> {
            saleBP.setText(String.format(Locale.US, "%.0f", dbHelper.getSingleBuyingPrice(parent.getItemAtPosition(position).toString())));
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
        btnScanSale.setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) activity.startScanner(saleParticulars);
        });
    }

    private void calculateLiveTotals() {
        try {
            String qtyStr = saleQty.getText().toString().trim();
            String bpStr = saleBP.getText().toString().trim();
            String spStr = saleSP.getText().toString().trim();
            String paidStr = saleActualAmount.getText().toString().trim();

            int q = qtyStr.isEmpty() ? 0 : Integer.parseInt(qtyStr);
            double bp = bpStr.isEmpty() ? 0 : Double.parseDouble(bpStr);
            double sp = spStr.isEmpty() ? 0 : Double.parseDouble(spStr);
            double paid = paidStr.isEmpty() ? 0 : Double.parseDouble(paidStr);
            
            double expAmt = q * sp;
            double expProfit = (sp - bp) * q;
            double balance = expAmt - paid;
            double actProfit = expProfit - balance;

            txtLiveProfit.setText(String.format(Locale.US, "UGX %s", MainActivity.formatMoney(actProfit)));
            txtLiveBalance.setText(String.format(Locale.US, "UGX %s", MainActivity.formatMoney(balance)));
        } catch (Exception e) {
            txtLiveProfit.setText("UGX 0");
            txtLiveBalance.setText("UGX 0");
        }
    }

    public void refreshInventoryAutocompleteData() {
        inventoryProductList.clear();
        Cursor cursor = dbHelper.getAllData();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                inventoryProductList.add(cursor.getString(1));
            }
            cursor.close();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, inventoryProductList);
        saleParticulars.setAdapter(adapter);
    }

    private void saveSalesTransactionRecord() {
        String p = saleParticulars.getText().toString().trim();
        String qStr = saleQty.getText().toString();
        String bStr = saleBP.getText().toString();
        String sStr = saleSP.getText().toString();
        String aStr = saleActualAmount.getText().toString();
        String cust = saleCustomerName.getText().toString().trim();
        String ph = saleCustomerPhone.getText().toString().trim();

        if (p.isEmpty() || qStr.isEmpty() || bStr.isEmpty() || sStr.isEmpty() || aStr.isEmpty()) {
            Toast.makeText(getContext(), "Fill all sale fields", Toast.LENGTH_SHORT).show(); return;
        }

        try {
            int q = Integer.parseInt(qStr);
            double bp = Double.parseDouble(bStr), sp = Double.parseDouble(sStr), paid = Double.parseDouble(aStr);
            double expAmt = q * sp, expProfit = (sp - bp) * q, bal = expAmt - paid, actProfit = expProfit - bal;
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            String tag = inventoryProductList.contains(p) ? "In System Inventory" : "Untracked Item";

            if (dbHelper.insertSaleLog(date, p, q, bp, sp, expAmt, expProfit, paid, actProfit, bal, tag, cust.isEmpty() ? "Walk-in" : cust, ph, time)) {
                dbHelper.reduceStock(p, q);
                dbHelper.logActivity(currentUsername, "NEW SALE", "Item: " + p + ", Qty: " + q + ", Total: " + paid);
                
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                Toast.makeText(requireContext(), "Sale Logged Successfully", Toast.LENGTH_SHORT).show();
                clearSaleInputs();
            }
        } catch (Exception e) { Toast.makeText(getContext(), "Sale Save Error", Toast.LENGTH_SHORT).show(); }
    }

    private void clearSaleInputs() {
        saleParticulars.setText(""); saleQty.setText(""); saleBP.setText(""); saleSP.setText("");
        saleActualAmount.setText(""); saleCustomerName.setText(""); saleCustomerPhone.setText("");
    }
}
