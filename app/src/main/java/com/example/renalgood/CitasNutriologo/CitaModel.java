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

    public CitaModel() {
        // Constructor vacío requerido para Firestore
    }

    // Constructor completo
    public CitaModel(String id, String nutriologoId, String pacienteId,
                     String pacienteNombre, Date fecha, String hora, String estado) {
        this.id = id;
        this.nutriologoId = nutriologoId;
        this.pacienteId = pacienteId;
        this.pacienteNombre = pacienteNombre;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNutriologoId() { return nutriologoId; }
    public void setNutriologoId(String nutriologoId) { this.nutriologoId = nutriologoId; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getPacienteNombre() { return pacienteNombre != null ? pacienteNombre : "Paciente"; }
    public void setPacienteNombre(String pacienteNombre) { this.pacienteNombre = pacienteNombre; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    // Método especial para manejar el timestamp de Firestore
    public void setFecha(Timestamp timestamp) {
        if (timestamp != null) {
            this.fecha = timestamp.toDate();
        }
    }

    public String getHora() { return hora != null ? hora : "Hora no especificada"; }
    public void setHora(String hora) { this.hora = hora; }

    public String getEstado() { return estado != null ? estado : "pendiente"; }
    public void setEstado(String estado) { this.estado = estado; }
}