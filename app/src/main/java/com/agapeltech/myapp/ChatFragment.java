package com.agapeltech.myapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment implements ChatAdapter.ChatActionListener {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText editMessage;
    private ImageButton btnSend;
    
    private DatabaseReference chatRef;
    private String currentUserEmail;
    private String currentUserName;
    private String userRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.chat_recycler_view);
        editMessage = view.findViewById(R.id.edit_message);
        btnSend = view.findViewById(R.id.btn_send);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserEmail = prefs.getString("username", "Unknown");
        userRole = prefs.getString("role", "STAFF");
        currentUserName = extractNameFromEmail(currentUserEmail);

        boolean isAdmin = "MANAGER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList, currentUserEmail, isAdmin, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Helps with keyboard visibility
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        chatRef = FirebaseDatabase.getInstance().getReference("business_chat");

        btnSend.setOnClickListener(v -> sendMessage());

        listenForMessages();

        // Scroll to bottom when keyboard appears
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && messageList.size() > 0) {
                recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(messageList.size() - 1), 100);
            }
        });

        return view;
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("senderEmail", currentUserEmail);
        messageMap.put("senderName", currentUserName);
        messageMap.put("message", text);
        messageMap.put("timestamp", ServerValue.TIMESTAMP);
        messageMap.put("edited", false);
        messageMap.put("deleted", false);

        chatRef.push().setValue(messageMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                editMessage.setText("");
            } else {
                Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForMessages() {
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setMessageId(snapshot.getKey());
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage updatedMessage = snapshot.getValue(ChatMessage.class);
                if (updatedMessage != null) {
                    String id = snapshot.getKey();
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getMessageId().equals(id)) {
                            updatedMessage.setMessageId(id);
                            messageList.set(i, updatedMessage);
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String id = snapshot.getKey();
                for (int i = 0; i < messageList.size(); i++) {
                    if (messageList.get(i).getMessageId().equals(id)) {
                        messageList.remove(i);
                        adapter.notifyItemRemoved(i);
                        break;
                    }
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMessageLongClick(ChatMessage message) {
        boolean isMe = message.getSenderEmail().equalsIgnoreCase(currentUserEmail);
        boolean isAdmin = "MANAGER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole);

        String[] options;
        if (isMe && isAdmin) options = new String[]{"Edit Message", "Delete Message", "Clear All Chats (Admin)"};
        else if (isMe) options = new String[]{"Edit Message", "Delete Message"};
        else if (isAdmin) options = new String[]{"Delete Message", "Clear All Chats (Admin)"};
        else return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Message Options")
                .setItems(options, (dialog, which) -> {
                    String choice = options[which];
                    if (choice.equals("Edit Message")) showEditDialog(message);
                    else if (choice.equals("Delete Message")) deleteMessage(message);
                    else if (choice.equals("Clear All Chats (Admin)")) confirmClearAll();
                }).show();
    }

    private void showEditDialog(ChatMessage message) {
        EditText input = new EditText(requireContext());
        input.setText(message.getMessage());
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Message")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = input.getText().toString().trim();
                    if (!newText.isEmpty()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("message", newText);
                        updates.put("edited", true);
                        chatRef.child(message.getMessageId()).updateChildren(updates);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(ChatMessage message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("deleted", true);
        updates.put("message", "Message deleted");
        chatRef.child(message.getMessageId()).updateChildren(updates);
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Messages")
                .setMessage("Are you sure you want to delete the entire chat history?")
                .setPositiveButton("Clear All", (dialog, which) -> chatRef.removeValue())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String namePart = email.split("@")[0].replace(".", " ").replace("_", " ");
        String[] words = namePart.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
