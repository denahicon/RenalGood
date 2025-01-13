package com.example.renalgood.vinnutriologo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.Nutriologo.Nutriologo;
import com.example.renalgood.databinding.ActivityNutriologoProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

 public class ViewNutriologoProfileActivity extends AppCompatActivity {
    private ActivityNutriologoProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String nutriologoId;
    private DatabaseReference realTimeDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNutriologoProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        realTimeDb = FirebaseDatabase.getInstance().getReference();

        nutriologoId = getIntent().getStringExtra("nutriologoId");
        if (nutriologoId == null) {
            Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadNutriologoData();
        checkVinculacionStatus();
        setupButtons();
    }

    private void loadNutriologoData() {
        db.collection("nutriologos").document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Nutriologo nutriologo = document.toObject(Nutriologo.class);
                        if (nutriologo != null) {
                            binding.txtNombre.setText(nutriologo.getNombre());
                            binding.txtAreaEspecializacion.setText(nutriologo.getAreaEspecializacion());
                            binding.txtExperiencia.setText("Experiencia: " + nutriologo.getAnosExperiencia() + " años");
                            binding.txtDireccion.setText(nutriologo.getDireccionClinica());

                            if (nutriologo.getPhotoUrl() != null && !nutriologo.getPhotoUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(nutriologo.getPhotoUrl())
                                        .circleCrop()
                                        .into(binding.imgNutriologo);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error al cargar datos: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

     private void vincularConNutriologo() {
         String userId = mAuth.getCurrentUser().getUid();

         // Crear documento en la colección vinculaciones
         Map<String, Object> vinculacionData = new HashMap<>();
         vinculacionData.put("pacienteId", userId);
         vinculacionData.put("nutriologoId", nutriologoId);
         vinculacionData.put("fechaVinculacion", FieldValue.serverTimestamp());
         vinculacionData.put("estado", "activo");

         // Primero crear la vinculación
         db.collection("vinculaciones")
                 .add(vinculacionData)
                 .addOnSuccessListener(documentReference -> {
                     // Luego actualizar el documento del paciente
                     Map<String, Object> pacienteData = new HashMap<>();
                     pacienteData.put("nutriologoId", nutriologoId);
                     pacienteData.put("fechaVinculacion", FieldValue.serverTimestamp());

                     db.collection("pacientes").document(userId)
                             .update(pacienteData)
                             .addOnSuccessListener(aVoid -> {
                                 Toast.makeText(this, "Vinculación exitosa", Toast.LENGTH_SHORT).show();
                                 checkVinculacionStatus();
                             })
                             .addOnFailureListener(e -> Toast.makeText(this,
                                     "Error al actualizar paciente: " + e.getMessage(),
                                     Toast.LENGTH_SHORT).show());
                 })
                 .addOnFailureListener(e -> Toast.makeText(this,
                         "Error al crear vinculación: " + e.getMessage(),
                         Toast.LENGTH_SHORT).show());
     }

    private void iniciarChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("nutriologoId", nutriologoId);
        startActivity(intent);
    }

    private String getChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ?
                userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

     private void checkVinculacionStatus() {
         String userId = mAuth.getCurrentUser().getUid();
         db.collection("pacientes").document(userId)
                 .get()
                 .addOnSuccessListener(document -> {
                     if (document.exists()) {
                         String vinculadoNutriologoId = document.getString("nutriologoId");
                         if (vinculadoNutriologoId != null && vinculadoNutriologoId.equals(nutriologoId)) {
                             // Ya está vinculado
                             binding.btnVincular.setVisibility(View.GONE);
                             binding.btnDesvincular.setVisibility(View.VISIBLE); // Añadir este botón al layout
                             binding.btnIniciarChat.setVisibility(View.VISIBLE);
                         } else {
                             // No está vinculado
                             binding.btnVincular.setVisibility(View.VISIBLE);
                             binding.btnDesvincular.setVisibility(View.GONE);
                             binding.btnIniciarChat.setVisibility(View.GONE);
                         }
                     }
                 });
     }

     private void setupButtons() {
         binding.btnVincular.setOnClickListener(v -> vincularConNutriologo());
         binding.btnDesvincular.setOnClickListener(v -> desvincularDeNutriologo());
         binding.btnIniciarChat.setOnClickListener(v -> iniciarChat());
     }

     private void desvincularDeNutriologo() {
         String userId = mAuth.getCurrentUser().getUid();

         // Primero eliminar el documento de vinculación
         db.collection("vinculaciones")
                 .whereEqualTo("pacienteId", userId)
                 .whereEqualTo("nutriologoId", nutriologoId)
                 .get()
                 .addOnSuccessListener(querySnapshot -> {
                     for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                         doc.getReference().delete();
                     }

                     // Luego actualizar el documento del paciente
                     db.collection("pacientes").document(userId)
                             .update("nutriologoId", null)
                             .addOnSuccessListener(aVoid -> {
                                 Toast.makeText(this, "Desvinculación exitosa", Toast.LENGTH_SHORT).show();
                                 checkVinculacionStatus();
                             })
                             .addOnFailureListener(e -> Toast.makeText(this,
                                     "Error al desvincular: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                 })
                 .addOnFailureListener(e -> Toast.makeText(this,
                         "Error al desvincular: " + e.getMessage(), Toast.LENGTH_SHORT).show());
     }
}