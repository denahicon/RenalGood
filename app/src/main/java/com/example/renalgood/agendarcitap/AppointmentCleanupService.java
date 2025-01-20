package com.example.renalgood.agendarcitap;

import android.icu.text.SimpleDateFormat;
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

                                if (citaCalendar.before(currentTime)) {
                                    Log.d(TAG, "Eliminando cita pasada: " + document.getId());
                                    document.getReference().delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Cita pasada eliminada: " + document.getId());
                                                // Notificar al nutriólogo y al paciente si es necesario
                                                String pacienteId = document.getString("pacienteId");
                                                String nutriologoId = document.getString("nutriologoId");
                                                if (pacienteId != null && nutriologoId != null) {
                                                    notificarCitaEliminada(pacienteId, nutriologoId,
                                                            citaTimestamp.toDate(), hora);
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Log.e(TAG, "Error al eliminar cita pasada", e));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando hora de cita", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error en limpieza de citas", e));
    }

    private static void notificarCitaEliminada(String pacienteId, String nutriologoId,
                                               Date fecha, String hora) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaStr = dateFormat.format(fecha);

        // Notificar al paciente
        NotificationService.sendNotificationToUser(
                pacienteId,
                "patients",
                "appointment_expired",
                "Cita Finalizada",
                "Tu cita del " + fechaStr + " a las " + hora + " ha sido eliminada del sistema",
                ""
        );

        // Notificar al nutriólogo
        NotificationService.sendNotificationToUser(
                nutriologoId,
                "nutriologos",
                "appointment_expired",
                "Cita Finalizada",
                "La cita del " + fechaStr + " a las " + hora + " ha sido eliminada del sistema",
                ""
        );
    }
}