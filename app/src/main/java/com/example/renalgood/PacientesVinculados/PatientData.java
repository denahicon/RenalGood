package com.example.renalgood.PacientesVinculados;

public class PatientData {
    private String id;
    private String name;
    private int age;
    private String situacionClinica;
    private double peso;
    private double estatura;
    private String email;
    private String telefono;
    private String actividadFisica;

    // Constructor vacío requerido para Firestore
    public PatientData() {}

    public PatientData(String name, int age, String situacionClinica, double peso,
                       double estatura, String email, String telefono, String actividadFisica) {
        this.name = name;
        this.age = age;
        this.situacionClinica = situacionClinica;
        this.peso = peso;
        this.estatura = estatura;
        this.email = email;
        this.telefono = telefono;
        this.actividadFisica = actividadFisica;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSituacionClinica() {
        return situacionClinica;
    }

    public void setSituacionClinica(String situacionClinica) {
        this.situacionClinica = situacionClinica;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public double getEstatura() {
        return estatura;
    }

    public void setEstatura(double estatura) {
        this.estatura = estatura;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getActividadFisica() {
        return actividadFisica;
    }

    public void setActividadFisica(String actividadFisica) {
        this.actividadFisica = actividadFisica;
    }

    public String getClinicalSituation() {
        return situacionClinica;
    }
}