package com.example.renalgood.CitasNutriologo;

import com.google.firebase.Timestamp;
import java.util.Date;

public class CitaModel {
    private String id;
    private String nutriologoId;
    private String pacienteId;
    private String pacienteNombre;
    private Date fecha;
    private String hora;
    private String estado;
    private long timestamp;  // Añadido para manejar el timestamp de Firestore

    public CitaModel() {
        // Constructor vacío requerido para Firestore
    }

    // Getters y Setters normales
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNutriologoId() { return nutriologoId; }
    public void setNutriologoId(String nutriologoId) { this.nutriologoId = nutriologoId; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String pacienteNombre) { this.pacienteNombre = pacienteNombre; }

    public Date getFecha() {
        // Si no hay fecha pero hay timestamp, crear la fecha del timestamp
        if (fecha == null && timestamp != 0) {
            return new Date(timestamp);
        }
        return fecha;
    }

    public void setFecha(Date fecha) { this.fecha = fecha; }

    public String getHora() { return hora != null ? hora : "No especificada"; }
    public void setHora(String hora) { this.hora = hora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        // Actualizar también la fecha si no está establecida
        if (this.fecha == null && timestamp != 0) {
            this.fecha = new Date(timestamp);
        }
    }
}