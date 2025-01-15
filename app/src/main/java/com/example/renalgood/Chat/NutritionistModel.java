package com.example.renalgood.Chat;

public class NutritionistModel {
    private String nombre;
    private String areaEspecializacion;
    private String anosExperiencia;
    private String direccionClinica;
    private String selfieUrl;  // para la foto de perfil

    // Constructor vacío requerido por Firebase
    public NutritionistModel() {}

    // Getters con los nombres antiguos para mantener compatibilidad
    public String getName() { return nombre; }
    public String getSpecialization() { return areaEspecializacion; }
    public String getExperience() { return anosExperiencia; }
    public String getClinic() { return direccionClinica; }
    public String getProfilePicUrl() { return selfieUrl; }
}