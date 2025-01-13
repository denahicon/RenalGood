package com.example.renalgood.Nutriologo;

public class NutriologoData {
    private String id;
    private String nombre;
    private String numeroCedula;
    private String universidad;
    private String anoGraduacion;
    private String areaEspecializacion;
    private String anosExperiencia;
    private String direccionClinica;
    private String correo;  // Estaba duplicado
    private String photoUrl;
    private boolean verificado;

    // Constructor vacío necesario para Firestore
    public NutriologoData() {}

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNumeroCedula() { return numeroCedula; }
    public void setNumeroCedula(String numeroCedula) { this.numeroCedula = numeroCedula; }

    public String getUniversidad() { return universidad; }
    public void setUniversidad(String universidad) { this.universidad = universidad; }

    public String getAnoGraduacion() { return anoGraduacion; }
    public void setAnoGraduacion(String anoGraduacion) { this.anoGraduacion = anoGraduacion; }

    public String getAreaEspecializacion() { return areaEspecializacion; }
    public void setAreaEspecializacion(String areaEspecializacion) { this.areaEspecializacion = areaEspecializacion; }

    public String getAnosExperiencia() { return anosExperiencia; }
    public void setAnosExperiencia(String anosExperiencia) { this.anosExperiencia = anosExperiencia; }

    public String getDireccionClinica() { return direccionClinica; }
    public void setDireccionClinica(String direccionClinica) { this.direccionClinica = direccionClinica; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }
}