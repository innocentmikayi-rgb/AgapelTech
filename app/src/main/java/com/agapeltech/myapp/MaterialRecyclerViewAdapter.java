package com.agapeltech.myapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class MaterialRecyclerViewAdapter extends RecyclerView.Adapter<MaterialRecyclerViewAdapter.ViewHolder> {

    private ArrayList<HashMap<String, String>> dataList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public MaterialRecyclerViewAdapter(Context context, ArrayList<HashMap<String, String>> dataList, OnItemClickListener listener) {
        this.context = context;
        this.dataList = dataList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_material, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, String> item = dataList.get(position);
        
        holder.txtItemName.setText(item.get("name"));
        holder.txtCategory.setText(item.get("category"));
        holder.txtBuyPrice.setText(item.get("buy"));
        holder.txtSellPrice.setText(item.get("sell"));

        int qty = Integer.parseInt(item.get("qty_raw"));
        int threshold = Integer.parseInt(item.get("threshold"));

        holder.txtStockQty.setText("Stock: " + qty);
        
        if (qty <= threshold) {
            holder.txtStockQty.setTextColor(Color.RED);
            holder.txtStockQty.setBackgroundColor(Color.parseColor("#FFEBEE"));
        } else {
            holder.txtStockQty.setTextColor(Color.parseColor("#2E7D32"));
            holder.txtStockQty.setBackgroundColor(Color.parseColor("#E8F5E9"));
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
        TextView txtItemName, txtCategory, txtBuyPrice, txtSellPrice, txtStockQty;
        ImageView imgItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtItemName = itemView.findViewById(R.id.txtItemName);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtBuyPrice = itemView.findViewById(R.id.txtBuyPrice);
            txtSellPrice = itemView.findViewById(R.id.txtSellPrice);
            txtStockQty = itemView.findViewById(R.id.txtStockQty);
            imgItem = itemView.findViewById(R.id.imgItem);
        }
    }
}
