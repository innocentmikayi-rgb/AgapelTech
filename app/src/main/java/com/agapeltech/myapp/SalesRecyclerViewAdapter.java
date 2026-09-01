package com.agapeltech.myapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class SalesRecyclerViewAdapter extends RecyclerView.Adapter<SalesRecyclerViewAdapter.ViewHolder> {

    private ArrayList<HashMap<String, String>> dataList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public SalesRecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> dataList, OnItemClickListener listener) {
        this.context = context;
        this.dataList = dataList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sale, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, String> sale = dataList.get(position);
        
        holder.txtSaleParticulars.setText(sale.get("particulars"));
        holder.txtSaleDate.setText(sale.get("date"));
        holder.txtSaleTime.setText(sale.get("time"));
        
        String customer = sale.get("customer");
        String phone = sale.get("phone");
        if (customer == null || customer.isEmpty()) customer = "Walk-in";
        holder.txtSaleCustomer.setText("Customer: " + customer + (phone != null && !phone.isEmpty() ? " (" + phone + ")" : ""));

        String qty = sale.get("qty");
        String sp = sale.get("sp");
        holder.txtSaleDetails.setText(String.format(java.util.Locale.US, "Qty: %s x UGX %s", qty, sp));

        double balance = 0;
        try {
            String balStr = sale.get("balance");
            if (balStr != null) balance = Double.parseDouble(balStr);
        } catch (NumberFormatException e) {
            android.util.Log.e("SalesAdapter", "Error parsing balance");
        }
        if (balance > 0) {
            holder.txtSaleStatus.setText("CREDIT");
            holder.txtSaleStatus.setBackgroundColor(Color.parseColor("#C62828"));
            holder.txtSaleBalanceOwed.setVisibility(View.VISIBLE);
            holder.txtSaleBalanceOwed.setText("Balance Owed: UGX " + MainActivity.formatMoney(balance));
        } else {
            holder.txtSaleStatus.setText("PAID");
            holder.txtSaleStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
            holder.txtSaleBalanceOwed.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSaleParticulars, txtSaleDate, txtSaleTime, txtSaleCustomer, txtSaleDetails, txtSaleStatus, txtSaleBalanceOwed;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSaleParticulars = itemView.findViewById(R.id.txtSaleParticulars);
            txtSaleDate = itemView.findViewById(R.id.txtSaleDate);
            txtSaleTime = itemView.findViewById(R.id.txtSaleTime);
            txtSaleCustomer = itemView.findViewById(R.id.txtSaleCustomer);
            txtSaleDetails = itemView.findViewById(R.id.txtSaleDetails);
            txtSaleStatus = itemView.findViewById(R.id.txtSaleStatus);
            txtSaleBalanceOwed = itemView.findViewById(R.id.txtSaleBalanceOwed);
        }
    }
}
