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
        db.collection("citas")
                .whereEqualTo("estado", "pendiente")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        Timestamp appointmentTimestamp = document.getTimestamp("fecha");
                        String appointmentHour = document.getString("hora");

                        if (appointmentTimestamp != null && appointmentHour != null) {
                            if (isAppointmentConfirmationExpired(appointmentTimestamp, appointmentHour)) {
                                document.getReference().delete()
                                        .addOnSuccessListener(aVoid ->
                                                Log.d(TAG, "Deleted expired appointment: " + document.getId()))
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
        return isWithinTimeWindow(appointmentTimestamp, appointmentHour, 24);
    }

    public static boolean isAppointmentConfirmationExpired(Timestamp appointmentTimestamp, String appointmentHour) {
        return !isWithinTimeWindow(appointmentTimestamp, appointmentHour, 24);
    }


    public static void verifyAppointmentAvailability(FirebaseFirestore db,
                                                     Date fecha,
                                                     String hora,
                                                     String nutriologoId,
                                                     AppointmentCallback callback) {
        db.collection("citas")
                .whereEqualTo("fecha", new Timestamp(fecha))
                .whereEqualTo("hora", hora)
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "confirmada")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        callback.onError("El horario seleccionado no está disponible");
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError("Error al verificar disponibilidad: " + e.getMessage()));
    }
}