package com.example.renalgood.vinnutriologo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class VinculacionManager {
    private static final String TAG = "VinculacionManager";
    protected static final DatabaseReference realTimeDb = FirebaseDatabase.getInstance().getReference();
    protected static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Método para acceder a Firestore
    public static FirebaseFirestore getFirestore() {
        return db;
    }

    // Método para acceder a Realtime Database
    public static DatabaseReference getRealTimeDb() {
        return realTimeDb;
    }

    public static void vincularConNutriologo(Context context, String pacienteId, String nutriologoId, OnVinculacionListener listener) {
        Log.d(TAG, "Iniciando vinculación - PacienteId: " + pacienteId + ", NutriologoId: " + nutriologoId);

        Map<String, Object> vinculacionData = new HashMap<>();
        vinculacionData.put("pacienteId", pacienteId);
        vinculacionData.put("nutriologoId", nutriologoId);
        vinculacionData.put("fechaVinculacion", FieldValue.serverTimestamp());
        vinculacionData.put("estado", "activo");

        db.collection("vinculaciones")
                .add(vinculacionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Vinculación creada con ID: " + documentReference.getId());

                    Map<String, Object> pacienteData = new HashMap<>();
                    pacienteData.put("nutriologoId", nutriologoId);
                    pacienteData.put("fechaVinculacion", FieldValue.serverTimestamp());

                    db.collection("pacientes").document(pacienteId)
                            .update(pacienteData)
                            .addOnSuccessListener(aVoid -> {
                                crearChat(context, pacienteId, nutriologoId, listener);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) {
                                    listener.onError(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }

    private static void crearChat(Context context, String pacienteId, String nutriologoId, OnVinculacionListener listener) {
        String chatId = getChatId(pacienteId, nutriologoId);

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("pacienteId", pacienteId);
        chatData.put("nutriologoId", nutriologoId);
        chatData.put("createdAt", ServerValue.TIMESTAMP);

        realTimeDb.child("chats").child(chatId)
                .setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                    if (context != null) {
                        Toast.makeText(context, "Vinculación exitosa", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }

    public static void desvincular(String pacienteId, String nutriologoId, OnVinculacionListener listener) {
        db.collection("vinculaciones")
                .whereEqualTo("pacienteId", pacienteId)
                .whereEqualTo("nutriologoId", nutriologoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    querySnapshot.getDocuments().forEach(doc ->
                            doc.getReference().update("estado", "inactivo"));

                    db.collection("pacientes").document(pacienteId)
                            .update("nutriologoId", null)
                            .addOnSuccessListener(aVoid -> {
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) {
                                    listener.onError(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }

    public static String getChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    public interface OnVinculacionListener {
        void onSuccess();
        void onError(Exception e);
    }
}