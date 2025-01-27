package com.example.renalgood.agendarcitap;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentTimeSlots {
    private static final int OPENING_HOUR = 8;
    private static final int CLOSING_HOUR = 20;
    private static final int LUNCH_START = 15;
    private static final int LUNCH_END = 16;
    private static final int SLOT_DURATION = 60; // Duration in minutes

    public static boolean isValidAppointmentTime(Calendar calendar) {
        // Reset seconds and milliseconds
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (dayOfWeek == Calendar.SUNDAY) return false;
        if (hour < OPENING_HOUR || hour >= CLOSING_HOUR) return false;
        if (hour >= LUNCH_START && hour < LUNCH_END) return false;

        return true;
    }

    public static List<String> getAvailableTimeSlots(Calendar date) {
        List<String> timeSlots = new ArrayList<>();
        Calendar slotTime = (Calendar) date.clone();
        slotTime.set(Calendar.MINUTE, 0);
        slotTime.set(Calendar.SECOND, 0);

        for (int hour = OPENING_HOUR; hour < CLOSING_HOUR; hour++) {
            // Skip lunch break
            if (hour >= LUNCH_START && hour < LUNCH_END) {
                continue;
            }

            slotTime.set(Calendar.HOUR_OF_DAY, hour);
            if (isValidAppointmentTime(slotTime)) {
                timeSlots.add(String.format("%02d:00", hour));
            }
        }

        return timeSlots;
    }

    public static void verifyAppointmentAvailability(FirebaseFirestore db, Date fecha, String hora, String nutriologoId, String pacienteId, AppointmentCallback callback) {
        // Normalizar la fecha/hora de la cita
        Calendar appointmentCal = Calendar.getInstance();
        appointmentCal.setTime(fecha);
        String[] timeParts = hora.split(":");
        appointmentCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        appointmentCal.set(Calendar.MINUTE, 0);
        appointmentCal.set(Calendar.SECOND, 0);
        appointmentCal.set(Calendar.MILLISECOND, 0);

        // Verificar 24 horas de anticipación
        Calendar currentTime = Calendar.getInstance();
        currentTime.set(Calendar.SECOND, 0);
        currentTime.set(Calendar.MILLISECOND, 0);

        Calendar limitTime = (Calendar) appointmentCal.clone();
        limitTime.add(Calendar.HOUR_OF_DAY, -24);

        if (currentTime.after(limitTime)) {
            callback.onError("Las citas deben agendarse con al menos 24 horas de anticipación");
            return;
        }

        // Verificar si existe una cita confirmada
        db.collection("citas")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "confirmada")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        Timestamp citaTimestamp = doc.getTimestamp("fecha");
                        String citaHora = doc.getString("hora");

                        if (citaTimestamp != null && citaHora != null) {
                            Calendar citaCalendar = Calendar.getInstance();
                            citaCalendar.setTime(citaTimestamp.toDate());
                            citaCalendar.set(Calendar.SECOND, 0);
                            citaCalendar.set(Calendar.MILLISECOND, 0);

                            if (citaCalendar.get(Calendar.YEAR) == appointmentCal.get(Calendar.YEAR) &&
                                    citaCalendar.get(Calendar.MONTH) == appointmentCal.get(Calendar.MONTH) &&
                                    citaCalendar.get(Calendar.DAY_OF_MONTH) == appointmentCal.get(Calendar.DAY_OF_MONTH) &&
                                    citaHora.equals(hora)) {

                                callback.onError("Este horario ya está ocupado");
                                return;
                            }
                        }
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Error al verificar disponibilidad"));
    }

    public static void startPeriodicCleanup(Context context, FirebaseFirestore db) {
        Handler handler = new Handler(context.getMainLooper());
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                cleanupExpiredAppointments(db);
                handler.postDelayed(this, 15 * 60 * 1000); // Cada 15 minutos
            }
        };
        handler.post(cleanupTask);
    }

    public static void cleanupExpiredAppointments(FirebaseFirestore db) {
        Calendar calendar = Calendar.getInstance();
        Timestamp currentTimestamp = new Timestamp(calendar.getTime());

        db.collection("citas")
                .whereEqualTo("estado", "pendiente")
                .whereLessThan("fechaCreacion", currentTimestamp)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        Timestamp citaTimestamp = document.getTimestamp("fecha");
                        String hora = document.getString("hora");
                        Timestamp creationTimestamp = document.getTimestamp("fechaCreacion");

                        // Si ya pasaron 24 horas desde la creación
                        if (currentTimestamp.getSeconds() - creationTimestamp.getSeconds() > 24 * 60 * 60) {
                            String pacienteId = document.getString("pacienteId");
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            String fechaStr = dateFormat.format(citaTimestamp.toDate());

                            NotificationService.sendExpirationNotification(pacienteId, fechaStr, hora);
                            document.getReference().delete();
                        }
                    }
                });
    }

    public static boolean canCancelAppointment(Timestamp appointmentTimestamp, String appointmentHour) {
        Calendar appointmentCal = Calendar.getInstance();
        appointmentCal.setTime(appointmentTimestamp.toDate());

        String[] timeParts = appointmentHour.split(":");
        appointmentCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        appointmentCal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));

        Calendar currentTime = Calendar.getInstance();
        Calendar limitTime = Calendar.getInstance();
        limitTime.setTime(appointmentCal.getTime());
        limitTime.add(Calendar.HOUR_OF_DAY, -24);

        return currentTime.before(limitTime);
    }
}