package com.example.renalgood.mensaje;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class MensajeDetalleActivity extends AppCompatActivity {
    private static final String TAG = "MensajeDetalleActivity";
    private RecyclerView rvMensajes;
    private EditText etMensaje;
    private ImageButton btnEnviar;
    private CircleImageView profileImage;
    private String pacienteId;
    private String nutriologoId;
    private String chatId;
    private FirebaseFirestore db;
    private List<Mensaje> listaMensajes;
    private MensajeAdapter mensajeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensaje_detalle);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        nutriologoId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        pacienteId = getIntent().getStringExtra("pacienteId");
        String nombrePaciente = getIntent().getStringExtra("nombrePaciente");

        initializeViews();
        setupToolbar(nombrePaciente);
        setupRecyclerView();
        loadPacienteInfo();
        habilitarEnvioDeMensajes();
        loadMessages();
    }

    private void initializeViews() {
        rvMensajes = findViewById(R.id.rvMessages);
        etMensaje = findViewById(R.id.etMessage);
        btnEnviar = findViewById(R.id.btnSend);
        profileImage = findViewById(R.id.profileImage);
        listaMensajes = new ArrayList<>();
    }

    private void setupToolbar(String nombrePaciente) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(nombrePaciente);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        mensajeAdapter = new MensajeAdapter(listaMensajes, nutriologoId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMensajes.setLayoutManager(layoutManager);
        rvMensajes.setAdapter(mensajeAdapter);
    }

    private void loadPacienteInfo() {
        if (profileImage == null) {
            Log.e(TAG, "profileImage view is null");
            return;
        }

        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String selfieUrl = document.getString("selfieUrl");
                        String gender = document.getString("gender");

                        runOnUiThread(() -> {
                            try {
                                if (selfieUrl != null && !selfieUrl.isEmpty()) {
                                    // Verificar que el contexto y la vista aún existan
                                    if (!isFinishing() && profileImage != null) {
                                        Glide.with(this)
                                                .load(selfieUrl)
                                                .placeholder(R.drawable.default_profile)
                                                .error(R.drawable.default_profile)
                                                .into(profileImage);
                                    }
                                } else {
                                    // Verificar que la vista exista antes de usarla
                                    if (profileImage != null) {
                                        profileImage.setImageResource(
                                                gender != null && gender.equals("Hombre") ?
                                                        R.drawable.hombre : R.drawable.mujer
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error setting profile image", e);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading paciente info", e));
    }

    private void habilitarEnvioDeMensajes() {
        etMensaje.setEnabled(true);
        btnEnviar.setEnabled(true);
        btnEnviar.setOnClickListener(v -> {
            String messageText = etMensaje.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });
    }

    private void loadMessages() {
        chatId = getChatId(nutriologoId, pacienteId);
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatId)
                .child("messages");

        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                try {
                    Mensaje mensaje = snapshot.getValue(Mensaje.class);
                    if (mensaje != null) {
                        listaMensajes.add(mensaje);
                        mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                        rvMensajes.scrollToPosition(listaMensajes.size() - 1);
                    }
                } catch (Exception e) {
                    Log.e("MensajeDetalleActivity", "Error deserializing message", e);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading messages", error.toException());
            }
        });
    }

    private void sendMessage(String messageText) {
        Map<String, Object> message = new HashMap<>();
        message.put("mensaje", messageText);
        message.put("emisorId", nutriologoId);
        message.put("timestamp", ServerValue.TIMESTAMP);
        message.put("read", false);

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatId)
                .child("messages")
                .push();

        chatRef.setValue(message)
                .addOnSuccessListener(aVoid -> {
                    etMensaje.setText("");
                    rvMensajes.scrollToPosition(listaMensajes.size() - 1);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error al enviar mensaje: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private String getChatId(String nutriologoId, String pacienteId) {
        return nutriologoId.compareTo(pacienteId) < 0
                ? nutriologoId + "_" + pacienteId
                : pacienteId + "_" + nutriologoId;
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