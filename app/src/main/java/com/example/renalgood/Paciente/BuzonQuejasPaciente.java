package com.example.renalgood.Paciente;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.R;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.recetas.RecetasActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuzonQuejasPaciente extends AppCompatActivity {
    private EditText editTextComentario;
    private RadioGroup radioGroupTipo;
    private Button buttonEnviar;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BuzonQuejas", "Iniciando onCreate");
        setContentView(R.layout.activity_buzon_quejas_paciente);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Log.d("BuzonQuejas", "Iniciando initializeViews");
        initializeViews();
        Log.d("BuzonQuejas", "Iniciando setupListeners");
        setupListeners();
        Log.d("BuzonQuejas", "Iniciando setupNavigationListeners");
        setupNavigationListeners();
        Log.d("BuzonQuejas", "onCreate completado");
    }

    private void initializeViews() {
        try {
            editTextComentario = findViewById(R.id.editTextComentario);
            Log.d("BuzonQuejas", "EditText encontrado: " + (editTextComentario != null));

            radioGroupTipo = findViewById(R.id.radioGroupTipo);
            Log.d("BuzonQuejas", "RadioGroup encontrado: " + (radioGroupTipo != null));

            buttonEnviar = findViewById(R.id.buttonEnviar);
            Log.d("BuzonQuejas", "Button encontrado: " + (buttonEnviar != null));

            ivHome = findViewById(R.id.ivHome);
            ivLupa = findViewById(R.id.ivLupa);
            ivChef = findViewById(R.id.ivChef);
            ivMensaje = findViewById(R.id.ivMensaje);
            ivCarta = findViewById(R.id.ivCarta);
            ivCalendario = findViewById(R.id.ivCalendario);
            Log.d("BuzonQuejas", "Navigation icons encontrados");
        } catch (Exception e) {
            Log.e("BuzonQuejas", "Error en initializeViews: " + e.getMessage());
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
        ivHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, PacienteActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivLupa.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListadeAlimentosActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivChef.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecetasActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivMensaje.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivCarta.setImageResource(R.drawable.ic_email);
        ivCarta.setColorFilter(getResources().getColor(R.color.pink_strong));

        ivCalendario.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarioActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
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

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión para enviar un comentario", Toast.LENGTH_SHORT).show();
            buttonEnviar.setEnabled(true);
            return;
        }

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
        comentario.put("tipoUsuario", "paciente"); // Agregamos esta línea para identificar que es un paciente

        db.collection("comentariosPacientes") // Cambiamos la colección para separar los comentarios de pacientes
                .add(comentario)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Comentario enviado exitosamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonEnviar.setEnabled(true);
                });
    }

    private void limpiarFormulario() {
        editTextComentario.setText("");
        radioGroupTipo.clearCheck();
        buttonEnviar.setEnabled(true);
    }
}