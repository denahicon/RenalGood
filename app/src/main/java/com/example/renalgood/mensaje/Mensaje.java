package com.example.renalgood.mensaje;

import java.util.Date;

public class Mensaje{
    private String mensajeId;
    private String emisorId;
    private String mensaje;
    private Date timestamp;

    public Mensaje() {}

    public Mensaje(String mensajeId, String emisorId, String mensaje, Date timestamp) {
        this.mensajeId = mensajeId;
        this.emisorId = emisorId;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
    }

    // Getters
    public String getMensajeId() { return mensajeId; }
    public String getEmisorId() { return emisorId; }
    public String getMensaje() { return mensaje; }
    public Date getTimestamp() { return timestamp; }
}