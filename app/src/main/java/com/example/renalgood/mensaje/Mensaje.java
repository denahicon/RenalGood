package com.example.renalgood.mensaje;

public class Mensaje {
    private String mensaje;
    private boolean read;
    private String emisorId;
    private long timestamp;

    // Constructor vacío requerido por Firebase
    public Mensaje() {
        // Constructor vacío requerido por Firebase
    }

    // Constructor con parámetros para uso en la app
    public Mensaje(String mensaje, boolean read, String emisorId, long timestamp) {
        this.mensaje = mensaje;
        this.read = read;
        this.emisorId = emisorId;
        this.timestamp = timestamp;
    }

    // Getters y setters con nombres exactamente iguales a los campos en Firebase
    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getEmisorId() {
        return emisorId;
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
}