package com.example.renalgood.mensaje;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MensajeDetalleActivity extends AppCompatActivity {
    private RecyclerView rvMensajes;
    private EditText etMensaje;
    private ImageButton btnEnviar;
    private String pacienteId;
    private String nutriologoId;
    private String mensajeId;
    private FirebaseFirestore db;
    private List<Mensaje> listaMensajes;
    private MensajeAdapter mensajeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_mensaje_detalle);

            // Obtener IDs de los extras
            pacienteId = getIntent().getStringExtra("pacienteId");
            String nombrePaciente = getIntent().getStringExtra("nombrePaciente");

            // Configurar toolbar con el nombre del paciente
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);  // Ahora esto funcionará correctamente
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(nombrePaciente);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mensajeId = getMensajeId(nutriologoId, pacienteId);


        initializeViews();
        setupEnvioMensajes();
        loadMensajes();
        loadPacienteStatus();

    }

    private void initializeViews() {
        rvMensajes = findViewById(R.id.rvMessages);
        etMensaje = findViewById(R.id.etMessage);
        btnEnviar = findViewById(R.id.btnSend);

        rvMensajes.setLayoutManager(new LinearLayoutManager(this));
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes, nutriologoId);
        rvMensajes.setAdapter(mensajeAdapter);
    }

    private void setupEnvioMensajes() {
        btnEnviar.setOnClickListener(v -> {
            String mensaje = etMensaje.getText().toString().trim();
            if (!mensaje.isEmpty()) {
                enviarMensaje(mensaje);
            }
        });
    }

    private void actualizarUltimoMensaje(String mensaje) {
        Map<String, Object> ultimoMensaje = new HashMap<>();
        ultimoMensaje.put("ultimoMensaje", mensaje);
        ultimoMensaje.put("timestamp", FieldValue.serverTimestamp());

        db.collection("chatInfo")
                .document(mensajeId)
                .set(ultimoMensaje, SetOptions.merge());
    }

    private void loadMensajes() {
        String chatId = nutriologoId + "_" + pacienteId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatId)
                .child("messages");

        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Mensaje mensaje = snapshot.getValue(Mensaje.class);
                if (mensaje != null) {
                    listaMensajes.add(mensaje);
                    mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                    rvMensajes.scrollToPosition(listaMensajes.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void enviarMensaje(String mensaje) {
        String chatId = nutriologoId + "_" + pacienteId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatId)
                .child("messages")
                .push();

        Map<String, Object> mensajeMap = new HashMap<>();
        mensajeMap.put("mensaje", mensaje);
        mensajeMap.put("emisorId", nutriologoId);
        mensajeMap.put("timestamp", ServerValue.TIMESTAMP);
        mensajeMap.put("read", false);

        chatRef.setValue(mensajeMap);
    }

    private void loadPacienteStatus() {
        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("pacienteId", pacienteId)
                .get()
                .addOnSuccessListener(documents -> {
                    if (!documents.isEmpty()) {
                        DocumentSnapshot doc = documents.getDocuments().get(0);
                        if ("activo".equals(doc.getString("estado"))) {
                            enableChat();
                        } else {
                            disableChat();
                        }
                    }
                });
    }

    private void enableChat() {
        etMensaje.setEnabled(true);
        btnEnviar.setEnabled(true);
        etMensaje.setHint("Escribe un mensaje...");
    }

    private void disableChat() {
        etMensaje.setEnabled(false);
        btnEnviar.setEnabled(false);
        etMensaje.setHint("No puedes enviar mensajes a este paciente");
    }

    private String getMensajeId(String nutriologoId, String pacienteId) {
        return String.format("chat_%s_%s", nutriologoId, pacienteId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}