package com.example.renalgood.Nutriologo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class NutriologoRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public NutriologoRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.auth = auth;
        this.db = db;
        this.storage = FirebaseStorage.getInstance();
    }

    public void registerNutriologist(
            String nombre,
            String areaEspecializacion,
            String anosExperiencia,
            String direccionClinica,
            String correo,
            String contrasena,
            String photoUrl,
            OnSuccessListener onSuccessListener,
            OnFailureListener onFailureListener) {

        // Primero crear el usuario con email y contraseña
        auth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();

                    // Crear objeto con los datos del nutriólogo
                    Nutriologo nutriologo = new Nutriologo(
                            userId,
                            nombre,
                            areaEspecializacion,
                            anosExperiencia,
                            direccionClinica,
                            correo,
                            photoUrl
                    );

                    // Guardar datos en Firestore
                    db.collection("nutriologos")
                            .document(userId)
                            .set(nutriologo)
                            .addOnSuccessListener(aVoid -> onSuccessListener.onSuccess())
                            .addOnFailureListener(onFailureListener::onFailure);
                })
                .addOnFailureListener(onFailureListener::onFailure);
    }

    // Interfaces para manejar callbacks
    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnFailureListener {
        void onFailure(Exception e);
    }
}