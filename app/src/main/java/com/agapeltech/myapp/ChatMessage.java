package com.agapeltech.myapp;

public class ChatMessage {
    private String senderEmail;
    private String senderName;
    private String message;
    private long timestamp;

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String senderEmail, String senderName, String message, long timestamp) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
