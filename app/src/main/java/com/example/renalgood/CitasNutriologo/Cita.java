package com.example.renalgood.CitasNutriologo;

import java.util.Date;

public class Cita {
    private String id;
    private String pacienteId;
    private String pacienteNombre;
    private Date fecha;
    private String hora;
    private String estado;

    // Constructor vacío requerido para Firestore
    public Cita() {}

    public Cita(String id, String pacienteId, String pacienteNombre, Date fecha,
                String hora, String estado) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.pacienteNombre = pacienteNombre;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String pacienteNombre) { this.pacienteNombre = pacienteNombre; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}