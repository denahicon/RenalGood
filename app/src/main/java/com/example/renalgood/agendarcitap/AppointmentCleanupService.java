package com.example.renalgood.agendarcitap;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppointmentCleanupService {
    private static final String TAG = "AppointmentCleanup";

    public static void cleanupPastAppointments(FirebaseFirestore db) {
        if (db == null) {
            Log.e(TAG, "FirebaseFirestore instance is null");
            return;
        }

        Calendar currentTime = Calendar.getInstance();

        db.collection("citas")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Timestamp citaTimestamp = document.getTimestamp("fecha");
                        String hora = document.getString("hora");
                        String estado = document.getString("estado");

                        if (citaTimestamp != null && hora != null &&
                                (estado.equals("pendiente") || estado.equals("confirmada"))) {

                            Calendar citaCalendar = Calendar.getInstance();
                            citaCalendar.setTime(citaTimestamp.toDate());

                            try {
                                String[] horaParts = hora.split(":");
                                citaCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaParts[0]));
                                citaCalendar.set(Calendar.MINUTE, Integer.parseInt(horaParts[1]));

                                // Si es una cita pendiente y ya pasó la hora programada
                                if (estado.equals("pendiente") && citaCalendar.before(currentTime)) {
                                    String pacienteId = document.getString("pacienteId");
                                    String nutriologoId = document.getString("nutriologoId");

                                    // Eliminar la cita
                                    document.getReference().delete()
                                            .addOnSuccessListener(aVoid -> {
                                                // Notificar a ambos usuarios
                                                if (pacienteId != null && nutriologoId != null) {
                                                    notificarCitaExpirada(pacienteId, nutriologoId,
                                                            citaTimestamp.toDate(), hora);
                                                }
                                            });
                                }
                                // Si es una cita confirmada y ya pasó
                                else if (estado.equals("confirmada") && citaCalendar.before(currentTime)) {
                                    document.getReference().delete();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando hora de cita", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error en limpieza de citas", e));
    }

    private static void notificarCitaExpirada(String pacienteId, String nutriologoId,
                                              Date fecha, String hora) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaStr = dateFormat.format(fecha);

        // Notificar al paciente
        NotificationService.sendNotificationToUser(
                pacienteId,
                "patients",
                "appointment_expired",
                "Cita Finalizada",
                "Tu cita del " + fechaStr + " a las " + hora +
                        " ha expirado porque el nutriólogo no respondió a tiempo",
                ""
        );

        // Notificar al nutriólogo
        NotificationService.sendNotificationToUser(
                nutriologoId,
                "nutriologos",
                "appointment_expired",
                "Cita Expirada",
                "La cita del " + fechaStr + " a las " + hora +
                        " ha expirado porque no fue atendida a tiempo",
                ""
        );
    }

    public static void startPeriodicCleanup(Context context, FirebaseFirestore db) {
        Handler handler = new Handler(context.getMainLooper());
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                cleanupPastAppointments(db);
                handler.postDelayed(this, 15 * 60 * 1000); // Cada 15 minutos
            }
        };
        handler.post(cleanupTask);
    }
}