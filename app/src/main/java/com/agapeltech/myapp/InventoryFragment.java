package com.agapeltech.myapp;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class InventoryFragment extends Fragment {

    private EditText itemName, buyingPrice, sellingPrice, searchBox, itemQty, lowStockThreshold;
    private Spinner itemCategory;
    private ImageButton btnScanAdmin;
    private Button saveButton;
    private View adminInputArea, headerAdminArea;
    private ImageView imgArrowInventory;
    private RecyclerView recyclerView;

    private DBHelper dbHelper;
    private ArrayList<HashMap<String, String>> listData = new ArrayList<>();
    private MaterialRecyclerViewAdapter adapter;
    private String currentUserRole = "STAFF";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        dbHelper = new DBHelper(requireContext());
        currentUserRole = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("role", "STAFF");

        initViews(view);
        setupListeners();
        applyRoleRestrictions();
        loadFromSQLite();

        return view;
    }

    private void initViews(View v) {
        itemName = v.findViewById(R.id.itemName);
        buyingPrice = v.findViewById(R.id.buyingPrice);
        sellingPrice = v.findViewById(R.id.sellingPrice);
        searchBox = v.findViewById(R.id.searchBox);
        itemQty = v.findViewById(R.id.itemQty);
        lowStockThreshold = v.findViewById(R.id.lowStockThreshold);
        itemCategory = v.findViewById(R.id.itemCategory);
        btnScanAdmin = v.findViewById(R.id.btnScanAdmin);
        saveButton = v.findViewById(R.id.saveButton);
        adminInputArea = v.findViewById(R.id.adminInputArea);
        headerAdminArea = v.findViewById(R.id.headerAdminArea);
        imgArrowInventory = v.findViewById(R.id.imgArrowInventory);
        recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        setupCategorySpinner();
    }

    private void setupCategorySpinner() {
        String[] cats = {"General", "Cement/Building", "Plumbing", "Electrical", "Tools", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cats);
        itemCategory.setAdapter(catAdapter);
    }

    private void setupListeners() {
        headerAdminArea.setOnClickListener(v -> toggleAdminForm());
        
        saveButton.setOnClickListener(v -> {
            String name = itemName.getText().toString().trim();
            if (name.isEmpty()) return;
            try {
                double buy = Double.parseDouble(buyingPrice.getText().toString());
                double sell = Double.parseDouble(sellingPrice.getText().toString());
                int qty = Integer.parseInt(itemQty.getText().toString());
                int threshold = Integer.parseInt(lowStockThreshold.getText().toString());
                String cat = itemCategory.getSelectedItem().toString();

                dbHelper.insertOrUpdate(name, buy, sell, qty, cat, threshold);
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                Toast.makeText(requireContext(), "Product Saved", Toast.LENGTH_SHORT).show();
                clearInputs();
                loadFromSQLite();
                if (activity != null) activity.refreshInventoryAutocompleteData();
            } catch (Exception e) {
                android.util.Log.e("InventoryFragment", "Error saving item: " + e.getMessage());
                Toast.makeText(requireContext(), "Invalid inputs", Toast.LENGTH_SHORT).show();
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterResults(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnScanAdmin.setOnClickListener(v -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) activity.startScanner(itemName);
        });
    }

    private void toggleAdminForm() {
        if (adminInputArea.getVisibility() == View.VISIBLE) {
            adminInputArea.setVisibility(View.GONE);
            imgArrowInventory.setRotation(0);
        } else {
            adminInputArea.setVisibility(View.VISIBLE);
            imgArrowInventory.setRotation(180);
        }
    }

    private void applyRoleRestrictions() {
        boolean isManager = "MANAGER".equals(currentUserRole);
        headerAdminArea.setVisibility(isManager ? View.VISIBLE : View.GONE);
        adminInputArea.setVisibility(View.GONE); // Default to closed
    }

    public void loadFromSQLite() {
        try {
            listData.clear();
            Cursor cursor = dbHelper.getAllData(); 
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("name", cursor.getString(1)); 
                    map.put("buy", "Buy: UGX " + MainActivity.formatMoney(cursor.getDouble(2))); 
                    map.put("sell", "Sell: UGX " + MainActivity.formatMoney(cursor.getDouble(3)));
                    map.put("qty_raw", "" + cursor.getInt(6));
                    map.put("threshold", "" + cursor.getInt(7));
                    map.put("category", cursor.getString(8));
                    listData.add(map);
                }
                cursor.close();
            }
            adapter = new MaterialRecyclerViewAdapter(requireContext(), listData, this::showOptionsDialog);
            if (recyclerView != null) recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            android.util.Log.e("InventoryFragment", "Error loading data: " + e.getMessage());
        }
    }

    private void filterResults(String query) {
        ArrayList<HashMap<String, String>> filtered = new ArrayList<>();
        String q = query.toLowerCase();
        for (HashMap<String, String> item : listData) {
            String name = item.get("name");
            String cat = item.get("category");
            if ((name != null && name.toLowerCase().contains(q)) || (cat != null && cat.toLowerCase().contains(q))) {
                filtered.add(item);
            }
        }
        adapter = new MaterialRecyclerViewAdapter(requireContext(), filtered, position -> showOptionsDialogFromList(filtered, position));
        recyclerView.setAdapter(adapter);
    }

    private void showOptionsDialog(int position) {
        showOptionsDialogFromList(listData, position);
    }

    private void showOptionsDialogFromList(ArrayList<HashMap<String, String>> sourceList, final int position) {
        String[] options = {"Edit Item", "Delete Item"};
        new AlertDialog.Builder(requireContext()).setTitle("Manage Item").setItems(options, (dialog, which) -> {
            String name = sourceList.get(position).get("name");
            if (which == 0) {
                LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40,20,40,20);
                final EditText b = new EditText(requireContext()); b.setHint("Buy Price"); 
                final EditText s = new EditText(requireContext()); s.setHint("Sell Price");
                final EditText q = new EditText(requireContext()); q.setHint("Stock Quantity");
                final EditText t = new EditText(requireContext()); t.setHint("Alert Threshold");
                final Spinner cSpin = new Spinner(requireContext());
                String[] cats = {"General", "Cement/Building", "Plumbing", "Electrical", "Tools", "Other"};
                cSpin.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cats));

                Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT buying_price, selling_price, stock_qty, category, low_stock_threshold FROM materials WHERE item_name=?", new String[]{name});
                if(c.moveToFirst()) { 
                    b.setText(String.format(Locale.US, "%.0f", c.getDouble(0))); 
                    s.setText(String.format(Locale.US, "%.0f", c.getDouble(1)));
                    q.setText(String.valueOf(c.getInt(2)));
                    String currentCat = c.getString(3);
                    for(int i=0; i<cats.length; i++) if(Objects.equals(cats[i], currentCat)) cSpin.setSelection(i);
                    t.setText(String.valueOf(c.getInt(4)));
                }
                c.close();
                
                layout.addView(b); layout.addView(s); layout.addView(q); layout.addView(t); layout.addView(cSpin);

                new AlertDialog.Builder(requireContext()).setTitle("Edit Item: " + name).setView(layout).setPositiveButton("Update", (d, w) -> {
                    dbHelper.insertOrUpdate(name, Double.parseDouble(b.getText().toString()), Double.parseDouble(s.getText().toString()), Integer.parseInt(q.getText().toString()), cSpin.getSelectedItem().toString(), Integer.parseInt(t.getText().toString()));
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData();
                    loadFromSQLite();
                }).show();
            } else { 
                dbHelper.markForDeletion(name); 
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && NetworkHelper.isOnline(requireContext())) activity.syncOfflineData(); 
                loadFromSQLite(); 
            }
        }).show();
    }

    private void clearInputs() { 
        itemName.setText(""); buyingPrice.setText(""); sellingPrice.setText(""); 
        itemQty.setText(""); itemCategory.setSelection(0);
        lowStockThreshold.setText("");
    }
}
