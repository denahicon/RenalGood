package com.example.renalgood.mensaje;

public class Mensaje {
    private String message;
    private boolean read;
    private String senderId;
    private long timestamp;

    // Constructor
    public Mensaje(String message, boolean read, String senderId, long timestamp) {
        this.message = message;
        this.read = read;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    // Getter and Setter methods
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}