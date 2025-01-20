package com.example.renalgood.agendarcitap;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppointmentValidations {
    private static final String TAG = "AppointmentValidations";
    private static final int BUSINESS_HOURS_START = 9;
    private static final int BUSINESS_HOURS_END = 18;
    private static final int CONFIRMATION_WINDOW_HOURS = 24;

    public static void verifyAppointmentAvailability(FirebaseFirestore db,
                                                     Date fecha,
                                                     String hora,
                                                     String nutriologoId,
                                                     String pacienteId,
                                                     AppointmentCallback callback) {
        // Obtener la fecha y hora actual
        Calendar currentTime = Calendar.getInstance();
        Date currentDate = currentTime.getTime();

        // Consultar citas activas
        db.collection("citas")
                .whereEqualTo("pacienteId", pacienteId)
                .whereIn("estado", Arrays.asList("pendiente", "confirmada"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasActiveFutureAppointment = false;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Timestamp citaTimestamp = doc.getTimestamp("fecha");
                        String citaHora = doc.getString("hora");

                        if (citaTimestamp != null && citaHora != null) {
                            Calendar citaCalendar = Calendar.getInstance();
                            citaCalendar.setTime(citaTimestamp.toDate());

                            // Establecer la hora de la cita
                            try {
                                String[] horaParts = citaHora.split(":");
                                citaCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(horaParts[0]));
                                citaCalendar.set(Calendar.MINUTE, Integer.parseInt(horaParts[1]));

                                // Comparar con el tiempo actual
                                if (citaCalendar.after(currentTime)) {
                                    hasActiveFutureAppointment = true;
                                    break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing hora: " + citaHora, e);
                            }
                        }
                    }

                    if (hasActiveFutureAppointment) {
                        callback.onError("Ya tienes una cita futura activa. No puedes agendar múltiples citas.");
                        return;
                    }

                    // Verificar disponibilidad del horario específico
                    db.collection("citas")
                            .whereEqualTo("fecha", new Timestamp(fecha))
                            .whereEqualTo("hora", hora)
                            .whereEqualTo("nutriologoId", nutriologoId)
                            .whereEqualTo("estado", "confirmada")
                            .get()
                            .addOnSuccessListener(timeSlotSnapshot -> {
                                if (!timeSlotSnapshot.isEmpty()) {
                                    callback.onError("El horario seleccionado no está disponible");
                                    return;
                                }
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onError("Error al verificar disponibilidad: " + e.toString()));
                })
                .addOnFailureListener(e -> callback.onError("Error al verificar citas pendientes: " + e.toString()));
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
        calendar.add(Calendar.HOUR_OF_DAY, CONFIRMATION_WINDOW_HOURS);
        Timestamp limitTimestamp = new Timestamp(calendar.getTime());

        db.collection("citas")
                .whereEqualTo("estado", "pendiente")
                .whereLessThan("fechaCreacion", limitTimestamp)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    String pacienteId = document.getString("pacienteId");
                                    Timestamp citaTimestamp = document.getTimestamp("fecha");
                                    String hora = document.getString("hora");

                                    if (pacienteId != null && citaTimestamp != null && hora != null) {
                                        NotificationService.sendAppointmentExpiredNotification(
                                                pacienteId,
                                                document.getId(),
                                                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                        .format(citaTimestamp.toDate()),
                                                hora
                                        );
                                    }
                                });
                    }
                });
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

    public static boolean isValidAppointmentTime(Calendar selectedCalendar) {
        Calendar currentCalendar = Calendar.getInstance();

        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        currentCalendar.set(Calendar.MILLISECOND, 0);

        if (selectedCalendar.before(currentCalendar)) {
            return false;
        }

        int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = selectedCalendar.get(Calendar.MINUTE);
        if (hour < BUSINESS_HOURS_START || hour > BUSINESS_HOURS_END ||
                (hour == BUSINESS_HOURS_END && minute > 0)) {
            return false;
        }

        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
    }

    public static boolean canCancelAppointment(Timestamp appointmentTimestamp, String appointmentHour) {
        Calendar appointmentCal = getAppointmentCalendar(appointmentTimestamp, appointmentHour);
        if (appointmentCal == null) return false;

        Calendar currentCal = Calendar.getInstance();
        Calendar limitCal = (Calendar) appointmentCal.clone();
        limitCal.add(Calendar.HOUR_OF_DAY, -CONFIRMATION_WINDOW_HOURS);

        return currentCal.before(limitCal);
    }

    public static boolean requiresConfirmation(Timestamp appointmentTimestamp, String appointmentHour) {
        Calendar appointmentCal = getAppointmentCalendar(appointmentTimestamp, appointmentHour);
        if (appointmentCal == null) return false;

        Calendar currentCal = Calendar.getInstance();
        Calendar limitCal = (Calendar) appointmentCal.clone();
        limitCal.add(Calendar.HOUR_OF_DAY, -CONFIRMATION_WINDOW_HOURS);

        return currentCal.before(limitCal);
    }
}