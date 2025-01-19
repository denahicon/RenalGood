package com.example.renalgood.agendarcitap;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static void sendNotificationToUser(String userId, String userCollection,
                                               String type, String title, String message, String citaId) {
        Log.d(TAG, "Enviando notificación tipo " + type + " a usuario: " + userId);

        db.collection(userCollection)
                .document(userId)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    String fcmToken = docSnapshot.getString("fcmToken");
                    if (fcmToken != null) {
                        Map<String, String> data = new HashMap<>();
                        data.put("type", type);
                        data.put("title", title);
                        data.put("message", message);
                        data.put("citaId", citaId);

                        sendNotification(fcmToken, data);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error obteniendo token del usuario", e));
    }

    public static void sendConfirmationReminder(String nutriologoId, String citaId) {
        sendNotificationToUser(nutriologoId, "nutriologos",
                "confirmation_reminder",
                "Recordatorio de Confirmación",
                "Tienes una cita pendiente por confirmar",
                citaId);
    }

    public static void sendAppointmentConfirmation(String pacienteId, String citaId, String fecha, String hora) {
        sendNotificationToUser(pacienteId, "patients",
                "appointment_confirmation",
                "Cita Confirmada",
                "Tu cita para el " + fecha + " a las " + hora + " ha sido confirmada",
                citaId);
    }

    private static void sendNotification(String token, Map<String, String> data) {
        try {
            RemoteMessage.Builder builder = new RemoteMessage.Builder(token)
                    .setData(data);

            FirebaseMessaging.getInstance().send(builder.build());
            Log.d(TAG, "Notificación enviada exitosamente a: " + token);
        } catch (Exception e) {
            Log.e(TAG, "Error enviando notificación", e);
        }
    }

    public static void sendAppointmentExpiredNotification(String pacienteId, String citaId,
                                                          String fecha, String hora) {
        sendNotificationToUser(pacienteId, "patients",
                "appointment_expired",
                "Cita Expirada",
                "Tu cita para el " + fecha + " a las " + hora +
                        " ha sido cancelada porque no fue confirmada a tiempo",
                citaId);
    }

}