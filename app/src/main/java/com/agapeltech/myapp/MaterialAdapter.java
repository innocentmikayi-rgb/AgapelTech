package com.agapeltech.myapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class MaterialAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> listData;
    private String userRole;

    public MaterialAdapter(Context context, ArrayList<HashMap<String, String>> listData, String userRole) {
        this.context = context;
        this.listData = listData;
        this.userRole = userRole;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate our brand new item layout
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_material, parent, false);
        }

        // Bind the views matching item_material.xml
        ImageView imgItem = convertView.findViewById(R.id.imgItem);
        TextView txtItemName = convertView.findViewById(R.id.txtItemName);
        TextView txtBuyPrice = convertView.findViewById(R.id.txtBuyPrice);
        TextView txtSellPrice = convertView.findViewById(R.id.txtSellPrice);
        TextView txtCategory = convertView.findViewById(R.id.txtCategory);
        TextView txtStockQty = convertView.findViewById(R.id.txtStockQty);

        // Fetch the data mapping
        HashMap<String, String> item = listData.get(position);

        if (item != null) {
            txtItemName.setText(item.get("name"));
            txtBuyPrice.setText(item.get("buy"));
            txtSellPrice.setText(item.get("sell"));
            txtCategory.setText(item.get("category"));

            boolean isManager = "MANAGER".equals(userRole);
            txtBuyPrice.setVisibility(isManager ? View.VISIBLE : View.GONE);
            
            String qtyStr = item.get("qty_raw");
            String thresholdStr = item.get("threshold");
            int qty = 0;
            int threshold = 5;
            try { qty = Integer.parseInt(qtyStr != null ? qtyStr : "0"); } catch (Exception e) {}
            try { threshold = Integer.parseInt(thresholdStr != null ? thresholdStr : "5"); } catch (Exception e) {}
            txtStockQty.setText("Stock: " + qty);
            
            if (qty <= threshold) {
                txtStockQty.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.error_red));
                txtStockQty.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"));
            } else {
                txtStockQty.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.success_green));
                txtStockQty.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"));
            }
            
            imgItem.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }
}
