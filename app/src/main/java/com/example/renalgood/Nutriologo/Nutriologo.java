package com.example.renalgood.Nutriologo;

public class Nutriologo {
    private String id;
    private String nombre;
    private String areaEspecializacion;
    private String anosExperiencia;
    private String direccionClinica;
    private String correo;
    private String photoUrl;

    // Constructor vacío necesario para Firestore
    public Nutriologo() {}

    public Nutriologo(String id, String nombre, String areaEspecializacion,
                      String anosExperiencia, String direccionClinica,
                      String correo, String photoUrl) {
        this.id = id;
        this.nombre = nombre;
        this.areaEspecializacion = areaEspecializacion;
        this.anosExperiencia = anosExperiencia;
        this.direccionClinica = direccionClinica;
        this.correo = correo;
        this.photoUrl = photoUrl;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAreaEspecializacion() {
        return areaEspecializacion;
    }

    public void setAreaEspecializacion(String areaEspecializacion) {
        this.areaEspecializacion = areaEspecializacion;
    }

    public String getAnosExperiencia() {
        return anosExperiencia;
    }

    public void setAnosExperiencia(String anosExperiencia) {
        this.anosExperiencia = anosExperiencia;
    }

    public String getDireccionClinica() {
        return direccionClinica;
    }

    public void setDireccionClinica(String direccionClinica) {
        this.direccionClinica = direccionClinica;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
