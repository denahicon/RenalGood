package com.example.renalgood.auth;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseAuthHandler {
    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface OnPasswordResetComplete {
        void onComplete(boolean success, String message);
    }

    public FirebaseAuthHandler(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void handlePasswordReset(String email, OnPasswordResetComplete callback) {
        // Primero verificamos si el usuario existe en alguna de nuestras colecciones
        checkUserExists(email, exists -> {
            if (!exists) {
                callback.onComplete(false, "No se encontró ninguna cuenta con este correo electrónico");
                return;
            }

            // Si el usuario existe, enviamos el correo de recuperación
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> {
                        // Actualizamos el estado en Firestore
                        updatePasswordResetStatus(email, true, updateSuccess -> {
                            if (updateSuccess) {
                                callback.onComplete(true, null);
                            } else {
                                callback.onComplete(false, "Error actualizando el estado de recuperación");
                            }
                        });
                    })
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private void checkUserExists(String email, OnUserExistsCheck callback) {
        db.collection("patients")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(patientSnapshot -> {
                    if (!patientSnapshot.isEmpty()) {
                        callback.onComplete(true);
                        return;
                    }

                    // Si no está en patients, buscamos en doctors
                    db.collection("doctors")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener(doctorSnapshot ->
                                    callback.onComplete(!doctorSnapshot.isEmpty()))
                            .addOnFailureListener(e -> callback.onComplete(false));
                })
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    private void updatePasswordResetStatus(String email, boolean resetPending, OnStatusUpdate callback) {
        updateCollectionResetStatus("patients", email, resetPending, success -> {
            if (success) {
                callback.onComplete(true);
            } else {
                updateCollectionResetStatus("doctors", email, resetPending, callback);
            }
        });
    }

    private void updateCollectionResetStatus(String collection, String email,
                                             boolean resetPending, OnStatusUpdate callback) {
        db.collection(collection)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().get(0).getReference()
                                .update(
                                        "passwordResetPending", resetPending,
                                        "lastPasswordReset", com.google.firebase.Timestamp.now()
                                )
                                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                                .addOnFailureListener(e -> callback.onComplete(false));
                    } else {
                        callback.onComplete(false);
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    private interface OnUserExistsCheck {
        void onComplete(boolean exists);
    }

    private interface OnStatusUpdate {
        void onComplete(boolean success);
    }
}