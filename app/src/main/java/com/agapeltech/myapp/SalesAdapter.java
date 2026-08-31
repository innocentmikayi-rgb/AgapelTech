package com.agapeltech.myapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SalesAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> salesList;

    public SalesAdapter(Context context, ArrayList<HashMap<String, String>> salesList) {
        this.context = context;
        this.salesList = salesList;
    }

    @Override
    public int getCount() {
        return salesList.size();
    }

    @Override
    public Object getItem(int position) {
        return salesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_sale, parent, false);
        }

        TextView txtSaleParticulars = convertView.findViewById(R.id.txtSaleParticulars);
        TextView txtSaleDate = convertView.findViewById(R.id.txtSaleDate);
        TextView txtSaleTime = convertView.findViewById(R.id.txtSaleTime);
        TextView txtSaleCustomer = convertView.findViewById(R.id.txtSaleCustomer);
        TextView txtSaleDetails = convertView.findViewById(R.id.txtSaleDetails);
        TextView txtSaleStatus = convertView.findViewById(R.id.txtSaleStatus);
        TextView txtSaleBalanceOwed = convertView.findViewById(R.id.txtSaleBalanceOwed);

        HashMap<String, String> sale = salesList.get(position);

        if (sale != null) {
            if (txtSaleParticulars != null) txtSaleParticulars.setText(sale.get("particulars"));
            if (txtSaleDate != null) txtSaleDate.setText(sale.get("date"));
            if (txtSaleTime != null) txtSaleTime.setText(sale.get("time") != null ? sale.get("time") : "");
            
            String cust = sale.get("customer") != null ? sale.get("customer") : "Walk-in";
            String phone = sale.get("phone");
            if (phone != null && !phone.isEmpty()) cust += " (" + phone + ")";
            if (txtSaleCustomer != null) txtSaleCustomer.setText("Customer: " + cust);

            if (txtSaleDetails != null) txtSaleDetails.setText("Qty: " + sale.get("qty") + " x UGX " + sale.get("sp"));

            double balance = 0;
            try {
                balance = Double.parseDouble(sale.get("balance"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(8);

            if (balance <= 0) {
                if (txtSaleStatus != null) {
                    txtSaleStatus.setText("PAID");
                    txtSaleStatus.setTextColor(Color.WHITE);
                    shape.setColor(Color.parseColor("#2E7D32"));
                    txtSaleStatus.setBackground(shape);
                }
                if (txtSaleBalanceOwed != null) txtSaleBalanceOwed.setVisibility(View.GONE);
            } else {
                if (txtSaleStatus != null) {
                    txtSaleStatus.setText("CREDIT");
                    txtSaleStatus.setTextColor(Color.WHITE);
                    shape.setColor(Color.parseColor("#C62828"));
                    txtSaleStatus.setBackground(shape);
                }
                if (txtSaleBalanceOwed != null) {
                    txtSaleBalanceOwed.setText("Balance Owed: UGX " + MainActivity.formatMoney(balance));
                    txtSaleBalanceOwed.setVisibility(View.VISIBLE);
                }
            }
        }

        return convertView;
    }
}
