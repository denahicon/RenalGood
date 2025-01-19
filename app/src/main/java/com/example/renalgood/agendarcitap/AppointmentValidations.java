package com.example.renalgood.agendarcitap;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppointmentValidations {
    private static final String TAG = "AppointmentValidations";

    public interface AppointmentCallback {
        void onSuccess();
        void onError(String message);
    }

    private static Calendar getAppointmentCalendar(Timestamp timestamp, String hourStr) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp.toDate());

            if (hourStr != null) {
                SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date appointmentTime = hourFormat.parse(hourStr);
                if (appointmentTime != null) {
                    Calendar hourCal = Calendar.getInstance();
                    hourCal.setTime(appointmentTime);
                    calendar.set(Calendar.HOUR_OF_DAY, hourCal.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, hourCal.get(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                }
            }
            return calendar;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing appointment time", e);
            return null;
        }
    }

    public static void cleanupExpiredAppointments(FirebaseFirestore db) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 24); // Obtener fecha límite (24 horas desde ahora)
        Timestamp limitTimestamp = new Timestamp(calendar.getTime());

        db.collection("citas")
                .whereEqualTo("estado", "pendiente")
                .whereLessThan("fecha", limitTimestamp)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        Timestamp appointmentTimestamp = document.getTimestamp("fecha");
                        String appointmentHour = document.getString("hora");

                        if (appointmentTimestamp != null && appointmentHour != null) {
                            if (isAppointmentConfirmationExpired(appointmentTimestamp, appointmentHour)) {
                                document.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Deleted expired appointment: " + document.getId());
                                            // Enviar notificación al paciente
                                            String pacienteId = document.getString("pacienteId");
                                            if (pacienteId != null) {
                                                NotificationService.sendAppointmentExpiredNotification(
                                                        pacienteId,
                                                        document.getId(),
                                                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(appointmentTimestamp.toDate()),
                                                        appointmentHour
                                                );
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Error deleting expired appointment", e));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error in cleanup", e));
    }

    public static void startPeriodicCleanup(Context context, FirebaseFirestore db) {
        Handler handler = new Handler(context.getMainLooper());
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                cleanupExpiredAppointments(db);
                handler.postDelayed(this, 15 * 60 * 1000); // Revisar cada 15 minutos
            }
        };
        handler.post(cleanupTask);
    }

    private static boolean isWithinTimeWindow(Timestamp appointmentTimestamp, String appointmentHour, int hoursBeforeAppointment) {
        try {
            Calendar appointmentCal = getAppointmentCalendar(appointmentTimestamp, appointmentHour);
            if (appointmentCal == null) return false;

            Calendar limitCal = (Calendar) appointmentCal.clone();
            limitCal.add(Calendar.HOUR_OF_DAY, -hoursBeforeAppointment);
            Calendar currentCal = Calendar.getInstance();

            return currentCal.before(limitCal);
        } catch (Exception e) {
            Log.e(TAG, "Error checking time window", e);
            return false;
        }
    }

    public static boolean canCancelAppointment(Timestamp appointmentTimestamp, String appointmentHour) {
        try {
            Calendar appointmentCal = getAppointmentCalendar(appointmentTimestamp, appointmentHour);
            if (appointmentCal == null) return false;

            // Obtener el calendario actual
            Calendar currentCal = Calendar.getInstance();

            // Calcular el límite de 24 horas antes de la cita
            Calendar limitCal = (Calendar) appointmentCal.clone();
            limitCal.add(Calendar.HOUR_OF_DAY, -24);

            // Debug logs
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Log.d(TAG, "Hora actual: " + sdf.format(currentCal.getTime()));
            Log.d(TAG, "Hora de la cita: " + sdf.format(appointmentCal.getTime()));
            Log.d(TAG, "Límite para cancelar: " + sdf.format(limitCal.getTime()));
            Log.d(TAG, "¿Se puede cancelar? " + currentCal.before(appointmentCal));

            // La cita puede ser cancelada si la hora actual es anterior a la hora de la cita
            return currentCal.before(appointmentCal);
        } catch (Exception e) {
            Log.e(TAG, "Error checking cancellation window", e);
            return false;
        }
    }

    public static boolean isAppointmentConfirmationExpired(Timestamp appointmentTimestamp, String appointmentHour) {
        return !isWithinTimeWindow(appointmentTimestamp, appointmentHour, 24);
    }

    public static void verifyAppointmentAvailability(FirebaseFirestore db,
                                                     Date fecha,
                                                     String hora,
                                                     String nutriologoId,
                                                     AppointmentCallback callback) {
        // Primero verificar si hay citas confirmadas
        db.collection("citas")
                .whereEqualTo("fecha", new Timestamp(fecha))
                .whereEqualTo("hora", hora)
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "confirmada")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        callback.onError("El horario seleccionado no está disponible");
                        return;
                    }

                    // Luego verificar si el paciente ya tiene una cita pendiente
                    db.collection("citas")
                            .whereEqualTo("pacienteId", nutriologoId)
                            .whereEqualTo("estado", "pendiente")
                            .get()
                            .addOnSuccessListener(pendingSnapshot -> {
                                if (!pendingSnapshot.isEmpty()) {
                                    callback.onError("Ya tienes una cita pendiente. No puedes agendar múltiples citas.");
                                    return;
                                }
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onError("Error al verificar citas pendientes: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Error al verificar disponibilidad: " + e.getMessage()));
    }

    public static boolean isValidAppointmentTime(Calendar selectedCalendar) {
        Calendar currentCalendar = Calendar.getInstance();

        // Resetear segundos y milisegundos
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        currentCalendar.set(Calendar.MILLISECOND, 0);

        // Validar fecha pasada
        if (selectedCalendar.before(currentCalendar)) {
            return false;
        }

        // Validar horario laboral (9 AM - 6 PM)
        int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = selectedCalendar.get(Calendar.MINUTE);
        if (hour < 9 || hour > 18 || (hour == 18 && minute > 0)) {
            return false;
        }

        // Validar fin de semana
        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
    }
}