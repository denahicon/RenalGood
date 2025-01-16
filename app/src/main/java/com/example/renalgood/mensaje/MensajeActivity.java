package com.example.renalgood.mensaje;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renalgood.Nutriologo.NavigationHelper;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MensajeActivity extends AppCompatActivity {
    private RecyclerView rvMensajes;
    private List<MensajeList> mensajeList;
    private MensajeListAdapter mensajeAdapter;
    private FirebaseFirestore db;
    private String nutriologoId;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensajes);

        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupRecyclerView();
        loadChatsForNutriologo();
    }

    private void initializeViews() {
        rvMensajes = findViewById(R.id.rvChats);
        ImageView ivHome = findViewById(R.id.ivHome);
        ImageView ivMensaje = findViewById(R.id.ivMensaje);
        ImageView ivCalendario = findViewById(R.id.ivCalendario);
        ImageView ivPacientesVinculados = findViewById(R.id.group_2811039);
        ImageView ivCarta = findViewById(R.id.ivCarta);

        navigationHelper = new NavigationHelper(
                this, ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta
        );
        navigationHelper.setupNavigation("mensaje");
    }

    private void setupRecyclerView() {
        mensajeList = new ArrayList<>();
        mensajeAdapter = new MensajeListAdapter(mensajeList, mensaje -> {
            Intent intent = new Intent(this, MensajeDetalleActivity.class);
            intent.putExtra("pacienteId", mensaje.getPacienteId());
            intent.putExtra("nombrePaciente", mensaje.getNombre());
            startActivity(intent);
        });

        rvMensajes.setLayoutManager(new LinearLayoutManager(this));
        rvMensajes.setAdapter(mensajeAdapter);
    }

    private void loadChatsForNutriologo() {
        // Query vinculaciones activas del nutriólogo
        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "activo")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MensajeActivity", "Error loading chats: ", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String pacienteId = doc.getString("pacienteId");
                            if (pacienteId != null) {
                                loadPacienteInfo(pacienteId);
                            }
                        }
                    }
                });
    }

    private void loadPacienteInfo(String pacienteId) {
        // Obtener información del paciente desde la colección "patients"
        db.collection("patients")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(pacienteDoc -> {
                    if (pacienteDoc.exists()) {
                        // Obtener el nombre y la foto del paciente
                        String nombre = pacienteDoc.getString("name");
                        String profilePic = pacienteDoc.getString("selfieUrl");

                        // Si no se encuentra en "patients", buscar en "pacientes"
                        if (nombre == null) {
                            db.collection("pacientes")
                                    .document(pacienteId)
                                    .get()
                                    .addOnSuccessListener(pacienteDoc2 -> {
                                        if (pacienteDoc2.exists()) {
                                            String nombreAlt = pacienteDoc2.getString("nombre");
                                            String profilePicAlt = pacienteDoc2.getString("profilePic");

                                            // Obtener último mensaje
                                            cargarUltimoMensaje(pacienteId, nombreAlt, profilePicAlt);
                                        }
                                    });
                        } else {
                            // Obtener último mensaje
                            cargarUltimoMensaje(pacienteId, nombre, profilePic);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("MensajeActivity", "Error loading paciente info: ", e));
    }

    private void cargarUltimoMensaje(String pacienteId, String nombre, String profilePic) {
        String chatId = getChatId(nutriologoId, pacienteId);
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatId)
                .child("messages");

        chatRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String ultimoMensaje = "";
                long timestamp = 0;

                for (DataSnapshot messageSnap : snapshot.getChildren()) {
                    Map<String, Object> message = (Map<String, Object>) messageSnap.getValue();
                    if (message != null) {
                        ultimoMensaje = (String) message.get("mensaje");
                        timestamp = (long) message.get("timestamp");
                    }
                }

                // Crear objeto MensajeList con la información actualizada
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String hora = timestamp != 0 ? sdf.format(new Date(timestamp)) : "";

                MensajeList mensajeItem = new MensajeList(
                        pacienteId,
                        nombre != null ? nombre : "Sin nombre",
                        ultimoMensaje != null ? ultimoMensaje : "",
                        hora,
                        profilePic
                );

                // Actualizar UI
                updateChatList(mensajeItem);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MensajeActivity", "Error loading last message: ", error.toException());
            }
        });
    }

    private void updateChatList(MensajeList newMessage) {
        runOnUiThread(() -> {
            // Actualizar la lista manteniendo el orden
            int existingIndex = -1;
            for (int i = 0; i < mensajeList.size(); i++) {
                if (mensajeList.get(i).getPacienteId().equals(newMessage.getPacienteId())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex != -1) {
                mensajeList.set(existingIndex, newMessage);
            } else {
                mensajeList.add(newMessage);
            }

            // Ordenar por hora del último mensaje (más reciente primero)
            Collections.sort(mensajeList, (m1, m2) ->
                    m2.getHora().compareTo(m1.getHora()));

            mensajeAdapter.updateList(mensajeList);
        });
    }

    private String getChatId(String nutriologoId, String pacienteId) {
        return nutriologoId.compareTo(pacienteId) < 0
                ? nutriologoId + "_" + pacienteId
                : pacienteId + "_" + nutriologoId;
    }
}