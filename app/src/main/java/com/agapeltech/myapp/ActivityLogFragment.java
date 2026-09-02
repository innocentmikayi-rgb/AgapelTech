package com.agapeltech.myapp;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class ActivityLogFragment extends Fragment {

    private RecyclerView rvActivityLogs;
    private TextView txtEmptyLogs;
    private DBHelper dbHelper;
    private final ArrayList<HashMap<String, String>> logList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_log, container, false);
        
        dbHelper = new DBHelper(requireContext());
        rvActivityLogs = view.findViewById(R.id.rvActivityLogs);
        txtEmptyLogs = view.findViewById(R.id.txtEmptyLogs);
        rvActivityLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadLogs();
        
        return view;
    }

    private void loadLogs() {
        logList.clear();
        Cursor cursor = dbHelper.getAllActivities();
        if (cursor != null) {
            int emailIdx = cursor.getColumnIndex("user_email");
            int typeIdx = cursor.getColumnIndex("action_type");
            int detailsIdx = cursor.getColumnIndex("details");
            int timeIdx = cursor.getColumnIndex("timestamp");

            while (cursor.moveToNext()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("user", emailIdx != -1 ? cursor.getString(emailIdx) : "Unknown");
                map.put("action", typeIdx != -1 ? cursor.getString(typeIdx) : "ACTION");
                map.put("details", detailsIdx != -1 ? cursor.getString(detailsIdx) : "");
                map.put("time", timeIdx != -1 ? cursor.getString(timeIdx) : "");
                logList.add(map);
            }
            cursor.close();
        }
        
        if (logList.isEmpty()) {
            txtEmptyLogs.setVisibility(View.VISIBLE);
            rvActivityLogs.setVisibility(View.GONE);
        } else {
            txtEmptyLogs.setVisibility(View.GONE);
            rvActivityLogs.setVisibility(View.VISIBLE);
        }
        
        rvActivityLogs.setAdapter(new LogAdapter());
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item_activity, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> log = logList.get(position);
            holder.txtLogAction.setText(log.get("action"));
            holder.txtLogUser.setText(log.get("user"));
            holder.txtLogDetails.setText(log.get("details"));
            holder.txtLogTime.setText(log.get("time"));
        }

        @Override
        public int getItemCount() {
            return logList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtLogAction, txtLogUser, txtLogDetails, txtLogTime;
            ViewHolder(View v) {
                super(v);
                txtLogAction = v.findViewById(R.id.txtLogAction);
                txtLogUser = v.findViewById(R.id.txtLogUser);
                txtLogDetails = v.findViewById(R.id.txtLogDetails);
                txtLogTime = v.findViewById(R.id.txtLogTime);
            }
        }
    }
}
