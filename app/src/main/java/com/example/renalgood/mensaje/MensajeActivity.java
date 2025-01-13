package com.example.renalgood.mensaje;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private List<Mensaje> mensajeList;
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

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupNavigationListeners();

        cargarMensajesNutriologo(nutriologoId);

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
        List<MensajeList> mensajeListItems = new ArrayList<>();
        for (Mensaje mensaje : mensajes) {
            mensajeListItems.add(convertToMensajeList(mensaje));
        }

        mensajeAdapter.updateList(mensajeListItems);

        Log.d("MensajeActivity", "Lista de mensajes actualizada con " + mensajes.size() + " elementos");
    }

    private MensajeList convertToMensajeList(Mensaje mensaje) {
        String pacienteId = mensaje.getSenderId(); // Asumimos que senderId es el pacienteId
        String nombre = ""; // Aquí deberías obtener el nombre del paciente
        String profilePic = ""; // Aquí deberías obtener la imagen de perfil del paciente
        String ultimoMensaje = mensaje.getMessage();
        String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(mensaje.getTimestamp()));

        return new MensajeList(pacienteId, nombre, ultimoMensaje, hora, profilePic);
    }

    private void debugDatabase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("DEBUG", "Usuario actual (Nutriólogo): " + userId);

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

        Log.d("DEBUG_CHAT", "NutriologoId: " + nutriologoId);

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
        List<MensajeList> mensajeList = new ArrayList<>();

        mensajeAdapter = new MensajeListAdapter(mensajeList, mensaje -> {
            Intent intent = new Intent(this, MensajeDetalleActivity.class);
            intent.putExtra("pacienteId", mensaje.getPacienteId());
            intent.putExtra("nombrePaciente", mensaje.getNombre());
            startActivity(intent);
        });

        rvMensajes.setLayoutManager(new LinearLayoutManager(this));
        rvMensajes.setAdapter(mensajeAdapter);
    }

    private String getChatId(String nutriologoId, String pacienteId) {
        return nutriologoId.compareTo(pacienteId) < 0
                ? nutriologoId + "_" + pacienteId
                : pacienteId + "_" + nutriologoId;
    }
}