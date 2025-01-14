package com.example.renalgood.Notificaciones;

import android.util.Log;

import com.example.renalgood.CitasNutriologo.CitaModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class CitaNotificationHandler {
    private final FirebaseFirestore db;
    private final SimpleDateFormat dateFormat;

    public CitaNotificationHandler() {
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    public void manejarRespuestaCita(CitaModel cita, boolean aceptada) {
        // 1. Actualizar estado en Firestore
        String nuevoEstado = aceptada ? "confirmada" : "rechazada";

        db.collection("citas")
                .document(cita.getId())
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    // 2. Enviar notificación al paciente
                    enviarNotificacion(cita, aceptada);
                })
                .addOnFailureListener(e -> {
                    Log.e("CitaNotification", "Error actualizando cita", e);
                });
    }

    private void enviarNotificacion(CitaModel cita, boolean aceptada) {
        db.collection("users")
                .document(cita.getPacienteId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fcmToken = documentSnapshot.getString("fcmToken");
                    if (fcmToken != null) {
                        String titulo = aceptada ? "Cita Confirmada" : "Cita Rechazada";
                        String mensaje = aceptada ?
                                "Tu cita para el " + formatearFecha(cita) + " ha sido confirmada" :
                                "Tu cita para el " + formatearFecha(cita) + " ha sido rechazada. Por favor, agenda una nueva cita.";

                        // Enviar notificación FCM
                        Map<String, String> data = new HashMap<>();
                        data.put("title", titulo);
                        data.put("body", mensaje);
                        data.put("citaId", cita.getId());

                        FirebaseMessaging.getInstance()
                                .send(new RemoteMessage.Builder(fcmToken)
                                        .setData(data)
                                        .build());
                    }
                });
    }

    private String formatearFecha(CitaModel cita) {
        return dateFormat.format(cita.getFecha()) + " a las " + cita.getHora();
    }
}