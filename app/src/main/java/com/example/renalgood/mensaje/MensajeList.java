package com.example.renalgood.mensaje;

public class MensajeList {
    private String pacienteId;
    private String nombre;
    private String ultimoMensaje;
    private String hora;
    private String profilePic;

    public MensajeList(String pacienteId, String nombre, String ultimoMensaje, String hora, String profilePic) {
        this.pacienteId = pacienteId;
        this.nombre = nombre;
        this.ultimoMensaje = ultimoMensaje;
        this.hora = hora;
        this.profilePic = profilePic;
    }

    public String getPacienteId() { return pacienteId; }
    public String getNombre() { return nombre; }
    public String getUltimoMensaje() { return ultimoMensaje; }
    public String getHora() { return hora; }
    public String getProfilePic() { return profilePic; }
}