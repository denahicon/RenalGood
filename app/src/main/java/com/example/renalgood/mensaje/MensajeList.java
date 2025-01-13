package com.example.renalgood.mensaje;

public class MensajeList {
    private String pacienteId;
    private String nombre;
    private String ultimoMensaje;
    private String hora;
    private String profilePic;

    // Constructor
    public MensajeList(String pacienteId, String nombre, String ultimoMensaje, String hora, String profilePic) {
        this.pacienteId = pacienteId;
        this.nombre = nombre;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.profilePic = profilePic;
    }

    // Getter and Setter methods
    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
}