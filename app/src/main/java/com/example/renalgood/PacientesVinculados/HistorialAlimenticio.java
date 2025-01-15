package com.example.renalgood.PacientesVinculados;

import java.util.Date;

public class HistorialAlimenticio {
    private String id;
    private String pacienteId;
    private Date fecha;
    private String nombreComida;
    private String tipoComida; // Desayuno, Comida, Cena
    private double calorias;
    private double proteinas;
    private double lipidos;
    private double carbohidratos;
    private double potasio;
    private double fosforo;
    private double sodio;

    // Constructor vacío requerido para Firestore
    public HistorialAlimenticio() {}

    public HistorialAlimenticio(String pacienteId, String nombreComida, String tipoComida,
                                double calorias, double proteinas, double lipidos,
                                double carbohidratos, double potasio, double fosforo,
                                double sodio) {
        this.pacienteId = pacienteId;
        this.fecha = new Date();
        this.nombreComida = nombreComida;
        this.tipoComida = tipoComida;
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.lipidos = lipidos;
        this.carbohidratos = carbohidratos;
        this.potasio = potasio;
        this.fosforo = fosforo;
        this.sodio = sodio;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getNombreComida() {
        return nombreComida;
    }

    public void setNombreComida(String nombreComida) {
        this.nombreComida = nombreComida;
    }

    public String getTipoComida() {
        return tipoComida;
    }

    public void setTipoComida(String tipoComida) {
        this.tipoComida = tipoComida;
    }

    public double getCalorias() {
        return calorias;
    }

    public void setCalorias(double calorias) {
        this.calorias = calorias;
    }

    public double getProteinas() {
        return proteinas;
    }

    public void setProteinas(double proteinas) {
        this.proteinas = proteinas;
    }

    public double getLipidos() {
        return lipidos;
    }

    public void setLipidos(double lipidos) {
        this.lipidos = lipidos;
    }

    public double getCarbohidratos() {
        return carbohidratos;
    }

    public void setCarbohidratos(double carbohidratos) {
        this.carbohidratos = carbohidratos;
    }

    public double getPotasio() {
        return potasio;
    }

    public void setPotasio(double potasio) {
        this.potasio = potasio;
    }

    public double getFosforo() {
        return fosforo;
    }

    public void setFosforo(double fosforo) {
        this.fosforo = fosforo;
    }

    public double getSodio() {
        return sodio;
    }

    public void setSodio(double sodio) {
        this.sodio = sodio;
    }
}