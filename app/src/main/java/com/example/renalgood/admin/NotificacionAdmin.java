package com.example.renalgood.admin;

import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificacionAdmin {
    private String id;
    private String nutriologoId;
    private String nombre;
    private String numeroCedula;
    private String universidad;
    private String anoGraduacion;
    private String areaEspecializacion;
    private String anosExperiencia;
    private String direccionClinica;
    private String correo;
    private String mensaje;
    private boolean leida;
    private Date fecha;
    private String identificacionPath;
    private String selfiePath;
    private String profilePhotoPath;
    private String identificacionUrl;
    private String selfieUrl;
    private String photoUrl;

    public NotificacionAdmin() {}

    public NotificacionAdmin(String nutriologoId, String nombre, String numeroCedula,
                             String universidad, String anoGraduacion,
                             String areaEspecializacion, String anosExperiencia,
                             String direccionClinica, String correo, String mensaje) {
        this.nutriologoId = nutriologoId;
        this.nombre = nombre;
        this.numeroCedula = numeroCedula;
        this.universidad = universidad;
        this.anoGraduacion = anoGraduacion;
        this.areaEspecializacion = areaEspecializacion;
        this.anosExperiencia = anosExperiencia;
        this.direccionClinica = direccionClinica;
        this.correo = correo;
        this.mensaje = mensaje;
        this.leida = false;
    }

    public static void guardarSolicitud(NotificacionAdmin notificacion, FirebaseFirestore db, OnCompleteListener<Void> listener) {
        if (notificacion.getIdentificacionUrl() == null || notificacion.getSelfieUrl() == null) {
            Log.e("Registro", "Faltan URLs de imágenes");
            listener.onComplete(Tasks.forException(new Exception("Faltan URLs de imágenes")));
            return;
        }

        Map<String, Object> datos = notificacion.toMap();

        if (notificacion.getId() == null) {
            db.collection("notificaciones_admin")
                    .add(datos)
                    .addOnSuccessListener(documentReference -> {
                        String id = documentReference.getId();
                        notificacion.setId(id);
                        documentReference.update("id", id)
                                .addOnCompleteListener(listener);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Registro", "Error guardando solicitud: " + e.getMessage());
                        listener.onComplete(Tasks.forException(e));
                    });
        } else {
            db.collection("notificaciones_admin")
                    .document(notificacion.getId())
                    .set(datos)
                    .addOnCompleteListener(listener);
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("nutriologoId", nutriologoId);
        map.put("nombre", nombre);
        map.put("numeroCedula", numeroCedula);
        map.put("universidad", universidad);
        map.put("anoGraduacion", anoGraduacion);
        map.put("areaEspecializacion", areaEspecializacion);
        map.put("anosExperiencia", anosExperiencia);
        map.put("direccionClinica", direccionClinica);
        map.put("correo", correo);
        map.put("mensaje", mensaje);
        map.put("leida", leida);
        map.put("fecha", fecha);
        map.put("identificacionPath", identificacionPath);
        map.put("selfiePath", selfiePath);
        map.put("profilePhotoPath", profilePhotoPath);
        map.put("identificacionUrl", identificacionUrl);
        map.put("selfieUrl", selfieUrl);
        map.put("photoUrl", photoUrl);
        return map;
    }

    public Map<String, Object> toNutriologo() {
        Map<String, Object> nutriologo = new HashMap<>();
        nutriologo.put("id", nutriologoId);
        nutriologo.put("nombre", nombre);
        nutriologo.put("numeroCedula", numeroCedula);
        nutriologo.put("universidad", universidad);
        nutriologo.put("anoGraduacion", anoGraduacion);
        nutriologo.put("areaEspecializacion", areaEspecializacion);
        nutriologo.put("anosExperiencia", anosExperiencia);
        nutriologo.put("direccionClinica", direccionClinica);
        nutriologo.put("correo", correo);
        nutriologo.put("estado", "aprobado");
        nutriologo.put("verificado", true);
        nutriologo.put("identificacionPath", identificacionPath);
        nutriologo.put("selfiePath", selfiePath);
        nutriologo.put("profilePhotoPath", profilePhotoPath);
        nutriologo.put("identificacionUrl", identificacionUrl);
        nutriologo.put("selfieUrl", selfieUrl);
        nutriologo.put("photoUrl", photoUrl);
        return nutriologo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNutriologoId() {
        return nutriologoId;
    }

    public void setNutriologoId(String nutriologoId) {
        this.nutriologoId = nutriologoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNumeroCedula() {
        return numeroCedula;
    }

    public void setNumeroCedula(String numeroCedula) {
        this.numeroCedula = numeroCedula;
    }

    public String getUniversidad() {
        return universidad;
    }

    public void setUniversidad(String universidad) {
        this.universidad = universidad;
    }

    public String getAnoGraduacion() {
        return anoGraduacion;
    }

    public void setAnoGraduacion(String anoGraduacion) {
        this.anoGraduacion = anoGraduacion;
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

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getIdentificacionUrl() {
        return identificacionUrl;
    }

    public void setIdentificacionUrl(String identificacionUrl) {
        this.identificacionUrl = identificacionUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }

    public void setSelfieUrl(String selfieUrl) {
        this.selfieUrl = selfieUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getIdentificacionPath() {
        return identificacionPath;
    }

    public void setIdentificacionPath(String identificacionPath) {
        this.identificacionPath = identificacionPath;
    }

    public String getSelfiePath() {
        return selfiePath;
    }

    public void setSelfiePath(String selfiePath) {
        this.selfiePath = selfiePath;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    public String getStoragePath() {
        return "verificacion/" + this.getId();
    }
}