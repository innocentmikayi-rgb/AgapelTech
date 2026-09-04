package com.agapeltech.myapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class LoansFragment extends Fragment {

    private EditText borrowerName, borrowerPhone, loanAmount, loanDetails, searchBox;
    private Button saveButton;
    private View adminInputArea, headerAdminArea;
    private ImageView imgArrowLoan;
    private RecyclerView recyclerView;

    private DBHelper dbHelper;
    private ArrayList<HashMap<String, String>> listData = new ArrayList<>();
    private LoanRecyclerViewAdapter adapter;
    private String currentUserRole = "STAFF";
    private String currentUsername = "Unknown";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loans, container, false);

        dbHelper = new DBHelper(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("role", "STAFF");
        currentUsername = prefs.getString("username", "Unknown");

        initViews(view);
        setupListeners();
        applyRoleRestrictions();
        loadLoansFromSQLite();

        return view;
    }

    private void initViews(View v) {
        borrowerName = v.findViewById(R.id.loanBorrowerName);
        borrowerPhone = v.findViewById(R.id.loanBorrowerPhone);
        loanAmount = v.findViewById(R.id.loanAmount);
        loanDetails = v.findViewById(R.id.loanDetails);
        searchBox = v.findViewById(R.id.loanSearchBox);
        saveButton = v.findViewById(R.id.btnSaveLoan);
        adminInputArea = v.findViewById(R.id.loanAdminInputArea);
        headerAdminArea = v.findViewById(R.id.loanHeaderAdminArea);
        imgArrowLoan = v.findViewById(R.id.imgArrowLoan);
        recyclerView = v.findViewById(R.id.loanRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupListeners() {
        headerAdminArea.setOnClickListener(v -> toggleAdminForm());

        saveButton.setOnClickListener(v -> {
            String name = borrowerName.getText().toString().trim();
            if (name.isEmpty()) return;
            try {
                String phone = borrowerPhone.getText().toString();
                double amount = Double.parseDouble(loanAmount.getText().toString());
                String details = loanDetails.getText().toString();
                String date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());

                if (dbHelper.insertLoan(name, phone, amount, date, details)) {
                    dbHelper.logActivity(currentUsername, "NEW LOAN", "Borrower: " + name + ", Amt: " + amount);
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                    Toast.makeText(requireContext(), "Loan Recorded", Toast.LENGTH_SHORT).show();
                    clearInputs();
                    loadLoansFromSQLite();
                }
            } catch (Exception e) {
                Log.e("LoansFragment", "Error saving loan: " + e.getMessage());
                Toast.makeText(requireContext(), "Invalid inputs", Toast.LENGTH_SHORT).show();
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterResults(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void toggleAdminForm() {
        if (adminInputArea.getVisibility() == View.VISIBLE) {
            adminInputArea.setVisibility(View.GONE);
            imgArrowLoan.setRotation(0);
        } else {
            adminInputArea.setVisibility(View.VISIBLE);
            imgArrowLoan.setRotation(180);
        }
    }

    private void applyRoleRestrictions() {
        // Staff can add loans, but admin restricted actions happen in the options dialog
    }

    public void loadLoansFromSQLite() {
        new Thread(() -> {
            try {
                final ArrayList<HashMap<String, String>> tempData = new ArrayList<>();
                Cursor cursor = dbHelper.getAllLoans();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("id", String.valueOf(cursor.getInt(0)));
                        map.put("borrower", cursor.getString(1));
                        map.put("phone", cursor.getString(2));
                        map.put("amount", MainActivity.formatMoney(cursor.getDouble(3)));
                        map.put("balance", MainActivity.formatMoney(cursor.getDouble(4)));
                        map.put("date", cursor.getString(5));
                        map.put("details", cursor.getString(6));
                        map.put("status", cursor.getString(7));
                        tempData.add(map);
                    }
                    cursor.close();
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        listData.clear();
                        listData.addAll(tempData);
                        adapter = new LoanRecyclerViewAdapter(requireContext(), listData, this::showOptionsDialog);
                        recyclerView.setAdapter(adapter);
                    });
                }
            } catch (Exception e) {
                Log.e("LoansFragment", "Error loading loans: " + e.getMessage());
            }
        }).start();
    }

    private void filterResults(String query) {
        ArrayList<HashMap<String, String>> filtered = new ArrayList<>();
        String q = query.toLowerCase();
        for (HashMap<String, String> item : listData) {
            String borrower = item.get("borrower");
            if (borrower != null && borrower.toLowerCase().contains(q)) {
                filtered.add(item);
            }
        }
        adapter = new LoanRecyclerViewAdapter(requireContext(), filtered, position -> showOptionsDialogFromList(filtered, position));
        recyclerView.setAdapter(adapter);
    }

    private void showOptionsDialog(int position) {
        showOptionsDialogFromList(listData, position);
    }

    private void showOptionsDialogFromList(ArrayList<HashMap<String, String>> sourceList, int position) {
        HashMap<String, String> loan = sourceList.get(position);
        String borrower = loan.get("borrower");
        String id = loan.get("id");

        String[] baseOptions = {"Share Receipt", "Settle Debt", "Edit Loan", "Delete Loan"};
        ArrayList<String> optionsList = new ArrayList<>();
        optionsList.add("Share Receipt");
        optionsList.add("Settle Debt");
        
        if ("MANAGER".equals(currentUserRole)) {
            optionsList.add("Edit Loan");
            optionsList.add("Delete Loan");
        }

        final String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(requireContext()).setTitle("Manage Loan: " + borrower).setItems(options, (dialog, which) -> {
            String selected = options[which];
            if ("Share Receipt".equals(selected)) shareLoanReceipt(loan);
            else if ("Settle Debt".equals(selected)) showSettleDebtDialog(loan);
            else if ("Edit Loan".equals(selected)) showEditLoanDialog(loan);
            else if ("Delete Loan".equals(selected)) deleteLoanRecord(loan);
        }).show();
    }

    private void shareLoanReceipt(HashMap<String, String> loan) {
        String receipt = "LOAN RECEIPT - AGAPELTECH\n\n" +
                "Borrower: " + loan.get("borrower") + "\n" +
                "Date: " + loan.get("date") + "\n" +
                "Total Amount: UGX " + loan.get("amount") + "\n" +
                "Current Balance: UGX " + loan.get("balance") + "\n" +
                "Status: " + loan.get("status") + "\n" +
                "Details: " + loan.get("details") + "\n\n" +
                "Thank you for business!";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, receipt);
        startActivity(Intent.createChooser(shareIntent, "Share Receipt Via"));
    }

    private void showSettleDebtDialog(HashMap<String, String> loan) {
        final EditText input = new EditText(requireContext());
        input.setHint("Enter amount paid");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        new AlertDialog.Builder(requireContext()).setTitle("Settle Debt: " + loan.get("borrower")).setView(input).setPositiveButton("Settle", (d, w) -> {
            try {
                String loanId = loan.get("id");
                if (loanId == null) return;
                double paid = Double.parseDouble(input.getText().toString());
                dbHelper.settleLoan(Integer.parseInt(loanId), paid);
                dbHelper.logActivity(currentUsername, "SETTLE LOAN", "Borrower: " + loan.get("borrower") + ", Paid: " + paid);
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                loadLoansFromSQLite();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showEditLoanDialog(HashMap<String, String> loan) {
        LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40,20,40,20);
        final EditText name = new EditText(requireContext()); name.setText(loan.get("borrower")); layout.addView(name);
        final EditText phone = new EditText(requireContext()); phone.setText(loan.get("phone")); layout.addView(phone);
        
        String amtStr = loan.get("amount");
        final EditText amt = new EditText(requireContext()); amt.setText(amtStr != null ? amtStr.replace(",", "") : "0"); layout.addView(amt);
        
        String balStr = loan.get("balance");
        final EditText bal = new EditText(requireContext()); bal.setText(balStr != null ? balStr.replace(",", "") : "0"); layout.addView(bal);
        
        final EditText det = new EditText(requireContext()); det.setText(loan.get("details")); layout.addView(det);

        new AlertDialog.Builder(requireContext()).setTitle("Edit Loan").setView(layout).setPositiveButton("Update", (d, w) -> {
            String loanId = loan.get("id");
            if (loanId == null) return;
            dbHelper.updateLoanRecord(Integer.parseInt(loanId), name.getText().toString(), phone.getText().toString(), 
                Double.parseDouble(amt.getText().toString()), Double.parseDouble(bal.getText().toString()), det.getText().toString(), loan.get("status"));
            dbHelper.logActivity(currentUsername, "UPDATE LOAN", "Borrower: " + loan.get("borrower"));
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
            loadLoansFromSQLite();
        }).show();
    }

    private void deleteLoanRecord(HashMap<String, String> loan) {
        new AlertDialog.Builder(requireContext()).setTitle("Delete Loan").setMessage("Are you sure you want to delete this record?")
            .setPositiveButton("Delete", (d, w) -> {
                dbHelper.markLoanForDeletion(Integer.parseInt(loan.get("id")));
                dbHelper.logActivity(currentUsername, "DELETE LOAN", "Borrower: " + loan.get("borrower"));
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                loadLoansFromSQLite();
            }).setNegativeButton("Cancel", null).show();
    }

    private void clearInputs() {
        borrowerName.setText(""); borrowerPhone.setText(""); loanAmount.setText(""); loanDetails.setText("");
    }
}
