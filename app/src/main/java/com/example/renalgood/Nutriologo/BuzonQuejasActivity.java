package com.example.renalgood.Nutriologo;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BuzonQuejasActivity extends AppCompatActivity {
    private static final String TAG = "BuzonQuejas";
    private EditText editTextComentario;
    private RadioGroup radioGroupTipo;
    private Button buttonEnviar;
    private ImageView ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private NavigationHelper navigationHelper;
    private boolean isActivityActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buzon_quejas);
        Log.d(TAG, "Iniciando onCreate");

        isActivityActive = true;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupListeners();
        setupNavigationListeners();
        registerForNotifications();
        verificarNotificaciones();
    }

    private void initializeViews() {
        try {
            editTextComentario = findViewById(R.id.editTextComentario);
            radioGroupTipo = findViewById(R.id.radioGroupTipo);
            buttonEnviar = findViewById(R.id.buttonEnviar);

            // Inicializar íconos de navegación
            ivHome = findViewById(R.id.ivHome);
            ivMensaje = findViewById(R.id.ivMensaje);
            ivCalendario = findViewById(R.id.ivCalendario);
            ivPacientesVinculados = findViewById(R.id.group_2811039);
            ivCarta = findViewById(R.id.ivCarta);

            // Configurar el ícono de carta como seleccionado
            ivCarta.setImageResource(R.drawable.ic_email);
            ivCarta.setColorFilter(getResources().getColor(R.color.pink_strong));

            Log.d(TAG, "Vistas inicializadas correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error en initializeViews: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        buttonEnviar.setOnClickListener(v -> {
            if (validarFormulario()) {
                enviarComentario();
            }
        });
    }

    private void setupNavigationListeners() {
        navigationHelper = new NavigationHelper(
                this, ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta
        );
        navigationHelper.setupNavigation("buzon");
    }

    private void registerForNotifications() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            FirebaseMessaging.getInstance().subscribeToTopic("nutriologo_" + userId)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Suscrito exitosamente a notificaciones");
                        } else {
                            Log.e(TAG, "Error al suscribirse a notificaciones", task.getException());
                        }
                    });
        }
    }

    private boolean validarFormulario() {
        if (editTextComentario.getText().toString().trim().isEmpty()) {
            editTextComentario.setError("Por favor, escribe tu comentario");
            return false;
        }

        if (radioGroupTipo.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Por favor, selecciona el tipo de comentario", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void enviarComentario() {
        String texto = editTextComentario.getText().toString().trim();
        String tipo = radioGroupTipo.getCheckedRadioButtonId() == R.id.radioQueja ? "queja" : "sugerencia";

        buttonEnviar.setEnabled(false);

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión para enviar un comentario", Toast.LENGTH_SHORT).show();
            buttonEnviar.setEnabled(true);
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        // Obtener información adicional del nutriólogo
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    Map<String, Object> comentario = new HashMap<>();
                    String comentarioId = UUID.randomUUID().toString();
                    comentario.put("id", comentarioId);
                    comentario.put("userId", userId);
                    comentario.put("tipo", tipo);
                    comentario.put("texto", texto);
                    comentario.put("fecha", System.currentTimeMillis());
                    comentario.put("estado", "pendiente");
                    comentario.put("email", email != null ? email : "");
                    comentario.put("tipoUsuario", "nutriologo");
                    comentario.put("nombreUsuario", userDoc.getString("nombre"));

                    // Crear el mensaje que se enviará cuando el admin responda
                    Map<String, Object> mensaje = new HashMap<>();
                    mensaje.put("id", comentarioId);
                    mensaje.put("userId", userId);
                    mensaje.put("texto", "Su " + tipo + " ha sido recibida y será atendida pronto.");
                    mensaje.put("fecha", System.currentTimeMillis());
                    mensaje.put("leido", false);
                    mensaje.put("tipo", tipo);

                    // Guardar el comentario y el mensaje en una transacción
                    db.runTransaction(transaction -> {
                        // Guardar el comentario
                        transaction.set(db.collection("comentariosNutriologos").document(comentarioId), comentario);

                        // Guardar el mensaje inicial
                        transaction.set(db.collection("mensajesNutriologos").document(comentarioId), mensaje);

                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(BuzonQuejasActivity.this, "Comentario enviado exitosamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();

                        // Crear notificación para el admin
                        Map<String, Object> notificacionAdmin = new HashMap<>();
                        notificacionAdmin.put("tipo", "nuevo_comentario_nutriologo");
                        notificacionAdmin.put("comentarioId", comentarioId);
                        notificacionAdmin.put("userId", userId);
                        notificacionAdmin.put("mensaje", "Nueva " + tipo + " de nutriólogo: " + userDoc.getString("nombre"));
                        notificacionAdmin.put("fecha", System.currentTimeMillis());
                        notificacionAdmin.put("estado", "pendiente");

                        // Guardar la notificación para el admin
                        db.collection("notificacionesAdmin").add(notificacionAdmin);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(BuzonQuejasActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        buttonEnviar.setEnabled(true);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al obtener información del usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonEnviar.setEnabled(true);
                });
    }

    private void limpiarFormulario() {
        editTextComentario.setText("");
        radioGroupTipo.clearCheck();
        buttonEnviar.setEnabled(true);
    }

    private void verificarNotificaciones() {
        String userId = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("leida", false)
                .whereEqualTo("tipoUsuario", "nutriologo")  // Para asegurar que es una notificación para nutriólogo
                .addSnapshotListener((snapshots, error) -> {
                    if (!isActivityActive) return;

                    if (error != null) {
                        Log.w(TAG, "Error al escuchar cambios", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        mostrarNotificaciones(snapshots.getDocuments());
                    }
                });
    }

    private void mostrarNotificaciones(List<DocumentSnapshot> notificaciones) {
        if (!isActivityActive) return;

        runOnUiThread(() -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_notificacion);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            TextView mensajeNotificacion = dialog.findViewById(R.id.mensajeNotificacion);
            Button btnOk = dialog.findViewById(R.id.btnOk);

            // Obtener el primer mensaje no leído
            String mensaje = notificaciones.get(0).getString("mensaje");
            mensajeNotificacion.setText(mensaje != null ? mensaje : "Tu queja ha sido atendida por el administrador");

            btnOk.setOnClickListener(v -> {
                // Marcar todas las notificaciones como leídas
                for (DocumentSnapshot doc : notificaciones) {
                    doc.getReference().update("leida", true);
                }
                dialog.dismiss();
            });

            // Configurar el tamaño del diálogo
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(window.getAttributes());
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(layoutParams);
            }

            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        });
    }
}