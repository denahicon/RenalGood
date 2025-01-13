package com.example.renalgood.mensaje;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.Chat.ChatMessage;
import com.example.renalgood.Nutriologo.BuzonQuejasActivity;
import com.example.renalgood.Nutriologo.CitasActivity;
import com.example.renalgood.Nutriologo.NutriologoActivity;
import com.example.renalgood.Nutriologo.PacientesVinculadosActivity;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;



public class MensajeActivity extends AppCompatActivity {
    private RecyclerView rvMensajes;
    private List<MensajeList> mensajeList;
    private MensajeListAdapter mensajeAdapter;
    private FirebaseFirestore db;
    private ImageView ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta;
    private DatabaseReference mDatabase;
    private String nutriologoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensajes);

        mDatabase = FirebaseDatabase.getInstance("https://ya-basta-default-rtdb.firebaseio.com/").getReference();
        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Obtener el ID del nutriólogo autenticado

        cargarMensajesNutriologo(nutriologoId);
        initializeViews();
        setupFirebase();
        setupRecyclerView();
        loadMensajes();
        setupNavigationListeners();
        debugDatabase();
    }

    private void cargarMensajesNutriologo(String nutriologoId) {
        mDatabase.child("vinculaciones").orderByChild("nutriologoId").equalTo(nutriologoId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot vinculacionSnapshot : dataSnapshot.getChildren()) {
                            Vinculacion vinculacion = vinculacionSnapshot.getValue(Vinculacion.class);
                            if (vinculacion != null && "activo".equals(vinculacion.getEstado())) {
                                String chatId = vinculacion.getPacienteId() + "_" + nutriologoId;
                                cargarMensajes(chatId);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("MensajeActivity", "Error obteniendo vinculaciones.", databaseError.toException());
                    }
                });
    }

    private void cargarMensajes(String chatId) {
        mDatabase.child("chats").child(chatId).child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Mensaje> mensajes = new ArrayList<>();
                        for (DataSnapshot mensajeSnapshot : dataSnapshot.getChildren()) {
                            Mensaje mensaje = mensajeSnapshot.getValue(Mensaje.class);
                            mensajes.add(mensaje);
                        }
                        actualizarUIConMensajes(mensajes);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("MensajeActivity", "Error obteniendo mensajes.", databaseError.toException());
                    }
                });
    }

    private void actualizarUIConMensajes(List<Mensaje> mensajes) {
        // Código para actualizar la interfaz de usuario con los mensajes obtenidos
        // ...
        Log.d("MensajeActivity", "Lista de mensajes actualizada con " + mensajes.size() + " elementos");
    }

    private void debugDatabase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("DEBUG", "Usuario actual (Nutriólogo): " + userId);

        // Verificar vinculaciones
        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", userId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("DEBUG", "Total vinculaciones activas: " + querySnapshot.size());
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d("DEBUG", "Vinculación encontrada: " + doc.getId());
                        Log.d("DEBUG", "Datos: " + doc.getData());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG", "Error consultando vinculaciones: " + e.toString());
                });

        // Verificar chats existentes
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("chats");
        chatRef.get().addOnSuccessListener(snapshot -> {
            Log.d("DEBUG", "Total chats en Realtime Database: " + snapshot.getChildrenCount());
            for (DataSnapshot chat : snapshot.getChildren()) {
                Log.d("DEBUG", "Chat ID: " + chat.getKey());
                Log.d("DEBUG", "Chat Data: " + chat.getValue());
            }
        });
    }

    private void initializeViews() {
        rvMensajes = findViewById(R.id.rvChats);
        ivHome = findViewById(R.id.ivHome);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCalendario = findViewById(R.id.ivCalendario);
        ivPacientesVinculados = findViewById(R.id.ivPacientesVinculados);
        ivCarta = findViewById(R.id.ivCarta);
    }

    private void setupNavigationListeners() {
        ivHome.setOnClickListener(view -> {
            navigateToActivity(NutriologoActivity.class);
            highlightCurrentIcon(ivHome);
        });

        ivMensaje.setOnClickListener(view -> {
            highlightCurrentIcon(ivMensaje);
        });

        ivCalendario.setOnClickListener(view -> {
            navigateToActivity(CitasActivity.class);
            highlightCurrentIcon(ivCalendario);
        });

        ivPacientesVinculados.setOnClickListener(view -> {
            navigateToActivity(PacientesVinculadosActivity.class);
            highlightCurrentIcon(ivPacientesVinculados);
        });

        ivCarta.setOnClickListener(view -> {
            navigateToActivity(BuzonQuejasActivity.class);
            highlightCurrentIcon(ivCarta);
        });

        // Resaltar el ícono actual al iniciar
        highlightCurrentIcon(ivMensaje);
    }

    private void navigateToActivity(Class<?> destinationClass) {
        if (this.getClass() != destinationClass) {
            Intent intent = new Intent(this, destinationClass);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    private void highlightCurrentIcon(ImageView selectedIcon) {
        int defaultColor = ContextCompat.getColor(this, R.color.icon_default);
        int primaryColor = ContextCompat.getColor(this, R.color.primary);

        ivHome.setColorFilter(defaultColor);
        ivMensaje.setColorFilter(defaultColor);
        ivCalendario.setColorFilter(defaultColor);
        ivPacientesVinculados.setColorFilter(defaultColor);
        ivCarta.setColorFilter(defaultColor);

        selectedIcon.setColorFilter(primaryColor);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Añadir estos logs
        Log.d("DEBUG_CHAT", "NutriologoId: " + nutriologoId);

        // Verificar vinculaciones directamente
        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("DEBUG_CHAT", "Número de vinculaciones encontradas: " + querySnapshot.size());
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d("DEBUG_CHAT", "Vinculación encontrada - PacienteId: " + doc.getString("pacienteId"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG_CHAT", "Error al consultar vinculaciones: " + e.getMessage());
                });
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

    private void loadMensajes() {
        Log.d("MensajeActivity", "Cargando mensajes para nutriólogo: " + nutriologoId);

        // Mantener una lista temporal para acumular todos los mensajes
        List<MensajeList> tempMensajeList = new ArrayList<>();

        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "activo")
                .get()  // Cambiado de addSnapshotListener a get() para manejar mejor la sincronización
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d("MensajeActivity", "No se encontraron vinculaciones activas");
                        mensajeAdapter.notifyDataSetChanged();
                        return;
                    }

                    Log.d("MensajeActivity", "Vinculaciones encontradas: " + querySnapshot.size());
                    final int[] processedCount = {0};

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String pacienteId = doc.getString("pacienteId");
                        if (pacienteId != null) {
                            db.collection("pacientes")
                                    .document(pacienteId)
                                    .get()
                                    .addOnSuccessListener(pacienteDoc -> {
                                        if (pacienteDoc.exists()) {
                                            String nombre = pacienteDoc.getString("nombre");
                                            String profilePic = pacienteDoc.getString("profilePic");

                                            // Obtener el último mensaje
                                            String chatId = getChatId(nutriologoId, pacienteId);
                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("chats")
                                                    .child(chatId)
                                                    .child("messages")
                                                    .limitToLast(1)
                                                    .get()
                                                    .addOnSuccessListener(snapshot -> {
                                                        String lastMessage = "No hay mensajes";
                                                        String time = "";

                                                        for (DataSnapshot messageSnap : snapshot.getChildren()) {
                                                            ChatMessage message = messageSnap.getValue(ChatMessage.class);
                                                            if (message != null) {
                                                                lastMessage = message.getMessage();
                                                                Date messageDate = new Date(message.getTimestamp());
                                                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                                                time = sdf.format(messageDate);
                                                            }
                                                        }

                                                        MensajeList mensajeItem = new MensajeList(
                                                                pacienteId,
                                                                nombre,
                                                                lastMessage,
                                                                time,
                                                                profilePic
                                                        );

                                                        tempMensajeList.add(mensajeItem);
                                                        processedCount[0]++;

                                                        // Si es el último elemento, actualizar el adapter
                                                        if (processedCount[0] == querySnapshot.size()) {
                                                            mensajeList.clear();
                                                            mensajeList.addAll(tempMensajeList);
                                                            mensajeAdapter.notifyDataSetChanged();
                                                            Log.d("MensajeActivity", "Lista de mensajes actualizada con " + mensajeList.size() + " elementos");
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MensajeActivity", "Error cargando vinculaciones: ", e);
                    Toast.makeText(this, "Error al cargar mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPacienteInfo(String pacienteId) {
        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        String profilePic = documentSnapshot.getString("profilePic");
                        Log.d("MensajeActivity", "Información de paciente cargada: " + nombre);
                        String chatId = getChatId(nutriologoId, pacienteId);
                        loadLastMessage(chatId, pacienteId, nombre, profilePic);
                    } else {
                        Log.e("MensajeActivity", "No se encontró el paciente: " + pacienteId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MensajeActivity", "Error cargando info del paciente: " + e.toString());
                });
    }

    private void loadLastMessage(String chatId, String pacienteId, String nombre, String profilePic) {
        FirebaseDatabase.getInstance().getReference()
                .child("chats")
                .child(chatId)
                .child("messages")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String lastMessage = "";
                        String time = "";

                        for (DataSnapshot messageSnap : snapshot.getChildren()) {
                            ChatMessage message = messageSnap.getValue(ChatMessage.class);
                            if (message != null) {
                                lastMessage = message.getMessage();
                                Date messageDate = new Date(message.getTimestamp());
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                time = sdf.format(messageDate);
                            }
                        }

                        MensajeList mensajeItem = new MensajeList(
                                pacienteId,
                                nombre,
                                lastMessage.isEmpty() ? "No hay mensajes" : lastMessage,
                                time,
                                profilePic
                        );

                        if (!mensajeList.contains(mensajeItem)) {
                            mensajeList.add(mensajeItem);
                            mensajeAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MensajeActivity", "Error loading last message: " + error.getMessage());
                    }
                });
    }

    private String getChatId(String nutriologoId, String pacienteId) {
        return nutriologoId.compareTo(pacienteId) < 0
                ? nutriologoId + "_" + pacienteId
                : pacienteId + "_" + nutriologoId;
    }
}