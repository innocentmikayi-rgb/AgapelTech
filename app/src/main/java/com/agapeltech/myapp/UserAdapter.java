package com.agapeltech.myapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener actionListener;

    public interface OnUserActionListener {
        void onUserDelete(User user);
        void onUserEdit(User user);
    }

    public UserAdapter(List<User> userList, OnUserActionListener actionListener) {
        this.userList = userList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserEmail.setText(user.getEmail());
        holder.tvUserRole.setText("ROLE: " + user.getRole());
        
        holder.btnEditUser.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onUserEdit(user);
        });
        
        holder.btnDeleteUser.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onUserDelete(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserEmail, tvUserRole;
        Button btnDeleteUser, btnEditUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            btnEditUser = itemView.findViewById(R.id.btnEditUser);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}
