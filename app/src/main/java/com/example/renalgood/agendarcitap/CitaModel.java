package com.example.renalgood.agendarcitap;

public class CitaModel {
    private String pacienteId;
    private String nutriologoId;
    private long timestamp;
    private String estado;

    public CitaModel() {
        // Constructor vacío requerido para Firestore
    }

    public CitaModel(String pacienteId, String nutriologoId, long timestamp, String estado) {
        this.pacienteId = pacienteId;
        this.nutriologoId = nutriologoId;
        this.timestamp = timestamp;
        this.estado = estado;
    }

    // Getters y setters
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getNutriologoId() { return nutriologoId; }
    public void setNutriologoId(String nutriologoId) { this.nutriologoId = nutriologoId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}