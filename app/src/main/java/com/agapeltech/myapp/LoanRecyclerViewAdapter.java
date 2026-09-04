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
import java.util.Locale;

public class LoanRecyclerViewAdapter extends RecyclerView.Adapter<LoanRecyclerViewAdapter.LoanViewHolder> {

    private final Context context;
    private final ArrayList<HashMap<String, String>> loans;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public LoanRecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> loans, OnItemClickListener listener) {
        this.context = context;
        this.loans = loans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_loan, parent, false);
        return new LoanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
        HashMap<String, String> loan = loans.get(position);
        holder.txtBorrower.setText(loan.get("borrower"));
        holder.txtDate.setText(loan.get("date"));
        holder.txtDetails.setText(loan.get("details"));
        
        String amtStr = loan.get("amount");
        holder.txtAmount.setText(String.format("Amt: UGX %s", amtStr != null ? amtStr : "0"));
        
        String balStr = loan.get("balance");
        holder.txtBalance.setText(String.format("Bal: UGX %s", balStr != null ? balStr : "0"));
        
        String status = loan.get("status");
        holder.txtStatus.setText(status);
        
        if ("SETTLED".equals(status)) {
            holder.txtStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
            holder.txtBalance.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.txtStatus.setBackgroundColor(Color.parseColor("#9C27B0")); // Purple
            holder.txtBalance.setTextColor(Color.parseColor("#C62828")); // Red
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return loans.size();
    }

    public static class LoanViewHolder extends RecyclerView.ViewHolder {
        TextView txtBorrower, txtDate, txtDetails, txtAmount, txtBalance, txtStatus;

        public LoanViewHolder(@NonNull View v) {
            super(v);
            txtBorrower = v.findViewById(R.id.txtLoanBorrower);
            txtDate = v.findViewById(R.id.txtLoanDate);
            txtDetails = v.findViewById(R.id.txtLoanDetails);
            txtAmount = v.findViewById(R.id.txtLoanAmount);
            txtBalance = v.findViewById(R.id.txtLoanBalance);
            txtStatus = v.findViewById(R.id.txtLoanStatus);
        }
    }
}
