package com.example.renalgood.mensaje;

public class Mensaje {
    private String mensaje;
    private String emisorId;
    private long timestamp;
    private boolean read;

    // Constructor vacío requerido por Firebase
    public Mensaje() {
        // Constructor vacío requerido para Firebase
    }

    // Constructor con parámetros
    public Mensaje(String mensaje, String emisorId, long timestamp, boolean read) {
        this.mensaje = mensaje;
        this.emisorId = emisorId;
        this.timestamp = timestamp;
        this.read = read;
    }

    // Getters y setters
    public String getMensaje() {
        return mensaje != null ? mensaje : "";
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEmisorId() {
        return emisorId != null ? emisorId : "";
    }

    public void setEmisorId(String emisorId) {
        this.emisorId = emisorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}