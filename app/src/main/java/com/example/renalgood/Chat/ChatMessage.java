package com.example.renalgood.Chat;

public class ChatMessage {
    private String messageId;
    private String emisorId;  // Cambiado de senderId a emisorId
    private String mensaje;   // Cambiado de message a mensaje
    private long timestamp;
    private boolean read;

    public ChatMessage() {} // Constructor vacío para Firebase

    public ChatMessage(String emisorId, String mensaje, long timestamp) {
        this.emisorId = emisorId;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
        this.read = false;
    }

    // Getters y setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getEmisorId() { return emisorId; }
    public void setEmisorId(String emisorId) { this.emisorId = emisorId; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}