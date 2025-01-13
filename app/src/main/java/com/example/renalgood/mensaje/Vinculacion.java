package com.example.renalgood.mensaje;

class Vinculacion {
    private String estado;
    private String nutriologoId;
    private String pacienteId;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNutriologoId() {
        return nutriologoId;
    }

    public void setNutriologoId(String nutriologoId) {
        this.nutriologoId = nutriologoId;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }
}