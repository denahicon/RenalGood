package com.example.renalgood.Nutriologo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.CitasNutriologo.CitasActivity;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.R;
import com.example.renalgood.mensaje.MensajeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuzonQuejasActivity extends AppCompatActivity {
    private EditText editTextComentario;
    private RadioGroup radioGroupTipo;
    private Button buttonEnviar;
    private ImageView ivHome;
    private ImageView ivMensaje;
    private ImageView ivCalendario;
    private ImageView ivPacientesVinculados;
    private ImageView ivCarta;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buzon_quejas);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupListeners();
        setupNavigationListeners();
    }

    private void initializeViews() {
        editTextComentario = findViewById(R.id.editTextComentario);
        radioGroupTipo = findViewById(R.id.radioGroupTipo);
        buttonEnviar = findViewById(R.id.buttonEnviar);
        ivHome = findViewById(R.id.ivHome);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCalendario = findViewById(R.id.ivCalendario);
        ivPacientesVinculados = findViewById(R.id.group_2811039);
        ivCarta = findViewById(R.id.ivCarta);
    }

    private void setupListeners() {
        buttonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validarFormulario()) {
                    enviarComentario();
                }
            }
        });
    }

    private void setupNavigationListeners() {
        navigationHelper = new NavigationHelper(
                this, ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta
        );
        navigationHelper.setupNavigation("buzon");
    }

    private void verificarTipoUsuarioYNavegar() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String tipoUsuario = document.getString("tipoUsuario");
                        if (tipoUsuario != null) {
                            switch (tipoUsuario) {
                                case "Nutriologo":
                                    startActivity(new Intent(BuzonQuejasActivity.this, NutriologoActivity.class));
                                    break;
                                case "Paciente":
                                    startActivity(new Intent(BuzonQuejasActivity.this, PacienteActivity.class));
                                    break;
                                default:
                                    Toast.makeText(BuzonQuejasActivity.this, "Tipo de usuario no reconocido", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(BuzonQuejasActivity.this, "Error al verificar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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
        String tipo;

        if (radioGroupTipo.getCheckedRadioButtonId() == R.id.radioQueja) {
            tipo = "queja";
        } else if (radioGroupTipo.getCheckedRadioButtonId() == R.id.radioSugerencia) {
            tipo = "sugerencia";
        } else {
            return;
        }

        buttonEnviar.setEnabled(false);

        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        Map<String, Object> comentario = new HashMap<>();
        comentario.put("id", UUID.randomUUID().toString());
        comentario.put("userId", userId);
        comentario.put("tipo", tipo);
        comentario.put("texto", texto);
        comentario.put("fecha", System.currentTimeMillis());
        comentario.put("estado", "pendiente");
        comentario.put("email", email != null ? email : "");

        db.collection("comentarios")
                .add(comentario)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(BuzonQuejasActivity.this, "Comentario enviado exitosamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BuzonQuejasActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonEnviar.setEnabled(true);
                });
    }

    private void limpiarFormulario() {
        editTextComentario.setText("");
        radioGroupTipo.clearCheck();
        buttonEnviar.setEnabled(true);
    }
}