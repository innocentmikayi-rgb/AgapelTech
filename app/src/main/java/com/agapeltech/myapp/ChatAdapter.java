package com.agapeltech.myapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private String currentUserEmail;
    private boolean isAdmin;
    private ChatActionListener actionListener;

    public interface ChatActionListener {
        void onMessageLongClick(ChatMessage message);
    }

    public ChatAdapter(List<ChatMessage> messageList, String currentUserEmail, boolean isAdmin, ChatActionListener listener) {
        this.messageList = messageList;
        this.currentUserEmail = currentUserEmail;
        this.isAdmin = isAdmin;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        
        if (message.isDeleted()) {
            holder.textMessage.setText("This message was deleted");
            holder.textMessage.setTypeface(null, Typeface.ITALIC);
            holder.textMessage.setTextColor(Color.GRAY);
        } else {
            String msgText = message.getMessage();
            if (message.isEdited()) {
                msgText += " (edited)";
            }
            holder.textMessage.setText(msgText != null ? msgText : "");
            holder.textMessage.setTypeface(null, Typeface.NORMAL);
            holder.textMessage.setTextColor(Color.BLACK);
        }

        holder.textUser.setText(message.getSenderName() != null ? message.getSenderName() : "User");
        
        long time = message.getTimestamp();
        if (time == 0) time = System.currentTimeMillis();
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.textTime.setText(sdf.format(new Date(time)));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        boolean isMe = message.getSenderEmail() != null && message.getSenderEmail().equalsIgnoreCase(currentUserEmail);
        
        if (isMe) {
            params.gravity = Gravity.END;
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_out);
            holder.textUser.setVisibility(View.GONE);
        } else {
            params.gravity = Gravity.START;
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_in);
            holder.textUser.setVisibility(View.VISIBLE);
        }
        holder.messageContainer.setLayoutParams(params);

        holder.itemView.setOnLongClickListener(v -> {
            if (!message.isDeleted() && (isMe || isAdmin)) {
                if (actionListener != null) actionListener.onMessageLongClick(message);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textUser, textMessage, textTime;
        LinearLayout messageContainer;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textUser = itemView.findViewById(R.id.text_user);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
            messageContainer = itemView.findViewById(R.id.message_container);
        }
    }
}
