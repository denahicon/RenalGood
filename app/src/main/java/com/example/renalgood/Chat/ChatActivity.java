package com.example.renalgood.Chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.renalgood.R;
import com.example.renalgood.databinding.ActivityChatBinding;
import com.example.renalgood.vinnutriologo.NutriologosListActivity;
import com.example.renalgood.vinnutriologo.VinculacionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.Paciente.BuzonQuejasPaciente;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.recetas.RecetasActivity;
import com.google.firebase.firestore.DocumentSnapshot;

import android.widget.ImageView;
import android.widget.Toast;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private ActivityChatBinding binding;
    private FirebaseAuth mAuth;
    private ChatAdapter mAdapter;
    private String nutriologoId;
    private String chatRoomId;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Obtener nutriologoId del intent si viene de la vista de perfil
        nutriologoId = getIntent().getStringExtra("nutriologoId");
        if (nutriologoId != null) {
            Log.d(TAG, "NutriologoId desde intent: " + nutriologoId);
            showChatUI();
        } else {
            Log.d(TAG, "Verificando vinculación existente...");
            checkVinculacion();
        }
        setupUI();
        initNavigationViews();
        setupNavigationListeners();
        debugDatabase();
    }

    private void debugDatabase() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d("DEBUG", "Usuario actual (Paciente): " + userId);

        // Verificar vinculación actual
        VinculacionManager.getFirestore()
                .collection("vinculaciones")
                .whereEqualTo("pacienteId", userId)
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

        if (nutriologoId != null) {
            String chatId = VinculacionManager.getChatId(userId, nutriologoId);
            Log.d("DEBUG", "Verificando chat ID: " + chatId);

            VinculacionManager.getRealTimeDb()
                    .child("chats")
                    .child(chatId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        Log.d("DEBUG", "Chat existe: " + snapshot.exists());
                        if (snapshot.exists()) {
                            Log.d("DEBUG", "Datos del chat: " + snapshot.getValue());
                        }
                    });
        }
    }

    private void checkVinculacion() {
        String userId = mAuth.getCurrentUser().getUid();

        VinculacionManager.getFirestore()
                .collection("vinculaciones")
                .whereEqualTo("pacienteId", userId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        nutriologoId = queryDocumentSnapshots.getDocuments().get(0).getString("nutriologoId");
                        if (nutriologoId != null) {
                            Log.d(TAG, "Vinculación encontrada con nutriólogo: " + nutriologoId);
                            showChatUI();
                            setupChatHeader();
                        } else {
                            showNoVinculacionUI();
                        }
                    } else {
                        Log.d(TAG, "No se encontraron vinculaciones activas");
                        showNoVinculacionUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking vinculacion: ", e);
                    showNoVinculacionUI();
                });
    }

    private void initNavigationViews() {
        ivHome = findViewById(R.id.ivHome);
        ivLupa = findViewById(R.id.ivLupa);
        ivChef = findViewById(R.id.ivChef);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCarta = findViewById(R.id.ivCarta);
        ivCalendario = findViewById(R.id.ivCalendario);
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

        ivMensaje.setImageResource(R.drawable.ic_message);
        ivMensaje.setColorFilter(getResources().getColor(R.color.pink_strong));

        ivCarta.setOnClickListener(v -> {
            Intent intent = new Intent(this, BuzonQuejasPaciente.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        ivCalendario.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarioActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void setupUI() {
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(this));

        binding.btnSend.setOnClickListener(v -> {
            String messageText = binding.editMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
                binding.editMessage.setText("");
            }
        });

        binding.btnVincular.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, NutriologosListActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupChatHeader() {
        if (nutriologoId == null) return;

        VinculacionManager.getFirestore()
                .collection("nutriologos")
                .document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        binding.nutriologoName.setText(document.getString("nombre"));
                        binding.chatHeader.setVisibility(View.VISIBLE);

                        binding.btnUnlink.setOnClickListener(v -> {
                            showDesvinculacionDialog();
                        });
                    }
                });
    }

    private void showDesvinculacionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Desvincular Nutriólogo")
                .setMessage("¿Estás seguro de que quieres desvincular a este nutriólogo?")
                .setPositiveButton("Sí", (dialog, which) -> desvincular())
                .setNegativeButton("No", null)
                .show();
    }

    private void desvincular() {
        String userId = mAuth.getCurrentUser().getUid();

        VinculacionManager.desvincular(userId, nutriologoId, new VinculacionManager.OnVinculacionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ChatActivity.this,
                        "Nutriólogo desvinculado exitosamente", Toast.LENGTH_SHORT).show();
                showNoVinculacionUI();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ChatActivity.this,
                        "Error al desvincular: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoVinculacionUI() {
        binding.chatContainer.setVisibility(View.GONE);
        binding.noVinculacionContainer.setVisibility(View.VISIBLE);
        binding.chatHeader.setVisibility(View.GONE);
        nutriologoId = null;
    }

    private void showChatUI() {
        binding.chatContainer.setVisibility(View.VISIBLE);
        binding.noVinculacionContainer.setVisibility(View.GONE);
        binding.chatHeader.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();
        chatRoomId = VinculacionManager.getChatId(userId, nutriologoId);
        Log.d(TAG, "Generated chatRoomId: " + chatRoomId);

        setupChat();
    }

    private void setupChat() {
        mAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerChat.setLayoutManager(layoutManager);
        binding.recyclerChat.setAdapter(mAdapter);

        DatabaseReference chatRef = VinculacionManager.getRealTimeDb()
                .child("chats")
                .child(chatRoomId)
                .child("messages");

        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    mAdapter.addMessage(message);
                    binding.recyclerChat.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                    Log.d(TAG, "Message added: " + message.getMessage());
                } else {
                    Log.e(TAG, "Error: Message is null in onChildAdded");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                // Handle child changed if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // Handle child removed if needed
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // Handle child moved if needed
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error in chatRef: " + error.getMessage());
                Toast.makeText(ChatActivity.this, "Error loading chat: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String messageText) {
        String userId = mAuth.getCurrentUser().getUid();
        ChatMessage message = new ChatMessage(userId, messageText, System.currentTimeMillis());

        DatabaseReference chatRef = VinculacionManager.getRealTimeDb()
                .child("chats")
                .child(chatRoomId)
                .child("messages");

        chatRef.push()
                .setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");
                    binding.recyclerChat.scrollToPosition(mAdapter.getItemCount() - 1);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: ", e);
                    Toast.makeText(ChatActivity.this,
                            "Error al enviar mensaje: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}