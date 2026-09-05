package com.agapeltech.myapp;

public class ChatMessage {
    private String senderEmail;
    private String senderName;
    private String message;
    private long timestamp;
    private boolean edited;
    private boolean deleted;
    private String messageId; // Unique ID for updating/deleting

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String senderEmail, String senderName, String message, long timestamp) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.edited = false;
        this.deleted = false;
    }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
