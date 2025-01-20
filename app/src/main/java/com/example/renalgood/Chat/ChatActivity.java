package com.example.renalgood.Chat;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.Paciente.BuzonQuejasPaciente;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.R;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.databinding.ActivityChatBinding;
import com.example.renalgood.recetas.RecetasActivity;
import com.example.renalgood.vinnutriologo.NutriologosListActivity;
import com.example.renalgood.vinnutriologo.VinculacionManager;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.firebase.database.Query;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private ActivityChatBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ChatAdapter mAdapter;
    private String nutriologoId;
    private String chatRoomId;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private ChildEventListener messagesListener;
    private SharedPreferences prefs;
    private Gson gson;
    private static final int MESSAGE_LIMIT = 50;
    private static final long CACHE_DURATION = 1000 * 60 * 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        gson = new Gson();

        setupUI();
        initNavigationViews();
        setupNavigationListeners();

        nutriologoId = getIntent().getStringExtra("nutriologoId");
        if (nutriologoId != null) {
            showChatUI();
            setupChatHeader();
        } else {
            checkVinculacion();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            DatabaseReference baseRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("chats")
                    .child(chatRoomId)
                    .child("messages");
            baseRef.removeEventListener(messagesListener);
        }
    }

    private void checkVinculacion() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Verificando vinculación para usuario: " + userId);

        db.collection("vinculaciones")
                .whereEqualTo("pacienteId", userId)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        nutriologoId = document.getString("nutriologoId");
                        if (nutriologoId != null) {
                            Log.d(TAG, "Vinculación encontrada con nutriólogo: " + nutriologoId);
                            showChatUI();
                            setupChatHeader();
                        } else {
                            Log.d(TAG, "nutriologoId es null en el documento");
                            showNoVinculacionUI();
                        }
                    } else {
                        Log.d(TAG, "No se encontraron vinculaciones activas");
                        showNoVinculacionUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verificando vinculación: ", e);
                    showNoVinculacionUI();
                });
    }

    private void setupUI() {
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(this));

        binding.btnSend.setOnClickListener(v -> {
            String messageText = binding.editMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });

        binding.btnVincular.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, NutriologosListActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupChatHeader() {
        if (nutriologoId == null) {
            Log.d(TAG, "nutriologoId es null en setupChatHeader");
            return;
        }

        db.collection("nutriologos")
                .document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nombre = document.getString("nombre");
                        String selfieUrl = document.getString("selfieUrl");
                        String gender = document.getString("gender");

                        if (binding.nutriologoName != null && nombre != null) {
                            binding.nutriologoName.setText(nombre);
                        }

                        if (binding.profileImage != null) {
                            if (selfieUrl != null && !selfieUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(selfieUrl)
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(binding.profileImage);
                            } else {
                                binding.profileImage.setImageResource(
                                        gender != null && gender.equals("Hombre") ?
                                                R.drawable.hombre : R.drawable.mujer
                                );
                            }
                        }

                        binding.chatHeader.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando datos del nutriólogo: ", e));

        // Configurar click listeners
        if (binding.chatHeader != null) {
            binding.chatHeader.setOnClickListener(v -> showNutritionistProfile());
        }

        if (binding.btnUnlink != null) {
            binding.btnUnlink.setOnClickListener(v -> showDesvinculacionDialog());
        }
    }

    private void showNutritionistProfile() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_nutriologo_profile);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        CircleImageView profileImage = dialog.findViewById(R.id.ivNutriologoFoto);
        TextView tvNombre = dialog.findViewById(R.id.tvNombreNutriologo);
        TextView tvArea = dialog.findViewById(R.id.tvAreaEspecializacion);
        TextView tvExperiencia = dialog.findViewById(R.id.tvAnosExperiencia);
        TextView tvClinica = dialog.findViewById(R.id.tvDireccionClinica);

        if (nutriologoId == null) {
            Toast.makeText(this, "Error al obtener información del nutriólogo", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("nutriologos")
                .document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nombre = document.getString("nombre");
                        String area = document.getString("areaEspecializacion");
                        String experiencia = document.getString("anosExperiencia");
                        String clinica = document.getString("direccionClinica");
                        String selfieUrl = document.getString("selfieUrl");
                        String gender = document.getString("gender");
                        tvNombre.setText(nombre);
                        tvArea.setText(String.format("Área de especialización:\n%s", area));
                        tvExperiencia.setText(String.format("Años de experiencia:\n%s años", experiencia));
                        tvClinica.setText(String.format("Dirección de clínica:\n%s", clinica));

                        if (selfieUrl != null && !selfieUrl.isEmpty()) {
                            Glide.with(ChatActivity.this)
                                    .load(selfieUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .circleCrop()
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(
                                    gender != null && gender.equals("Hombre") ?
                                            R.drawable.hombre : R.drawable.mujer
                            );
                        }
                        dialog.show();
                    } else {
                        Toast.makeText(ChatActivity.this,
                                "No se encontró información del nutriólogo",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar el perfil", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading nutritionist profile", e);
                });
    }

    private void showChatUI() {
        binding.chatContainer.setVisibility(View.VISIBLE);
        binding.noVinculacionContainer.setVisibility(View.GONE);
        binding.chatHeader.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();
        chatRoomId = VinculacionManager.getChatId(userId, nutriologoId);
        Log.d(TAG, "Generated chatRoomId: " + chatRoomId);

        loadPatientInfo(); // Añadir esta línea
        setupChat();
    }

    private void setupChat() {
        if (chatRoomId == null) {
            Log.e(TAG, "ChatRoomId is null");
            return;
        }

        // Configurar RecyclerView
        mAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerChat.setLayoutManager(layoutManager);
        binding.recyclerChat.setAdapter(mAdapter);

        // Cargar mensajes en cache primero
        loadCachedMessages();

        // Obtener referencia base
        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatRoomId)
                .child("messages");

        // Configurar query con límite
        Query chatQuery = baseRef.limitToLast(MESSAGE_LIMIT);

        // Crear listener para nuevos mensajes
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                try {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        // Guardar mensaje en cache
                        saveMessageToCache(message);

                        // Actualizar UI
                        mAdapter.addMessage(message);
                        smoothScrollToBottom();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing message", e);
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
                Log.e(TAG, "Error en chatRef: " + error.getMessage());
            }
        };

        // Agregar listener a la query
        chatQuery.addChildEventListener(messagesListener);
    }

    private void loadCachedMessages() {
        String cachedMessages = prefs.getString("chat_" + chatRoomId, null);
        if (cachedMessages != null) {
            try {
                Type listType = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
                List<ChatMessage> messages = gson.fromJson(cachedMessages, listType);
                if (messages != null) {
                    for (ChatMessage message : messages) {
                        mAdapter.addMessage(message);
                    }
                    smoothScrollToBottom();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading cached messages", e);
            }
        }
    }

    private void saveMessageToCache(ChatMessage message) {
        try {
            List<ChatMessage> cachedMessages = getCachedMessages();
            cachedMessages.add(message);
            while (cachedMessages.size() > MESSAGE_LIMIT) {
                cachedMessages.remove(0);
            }
            String json = gson.toJson(cachedMessages);
            prefs.edit().putString("chat_" + chatRoomId, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving message to cache", e);
        }
    }

    private List<ChatMessage> getCachedMessages() {
        String cachedMessages = prefs.getString("chat_" + chatRoomId, null);
        if (cachedMessages != null) {
            try {
                Type listType = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
                List<ChatMessage> messages = gson.fromJson(cachedMessages, listType);
                if (messages != null) {
                    return messages;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting cached messages", e);
            }
        }
        return new ArrayList<>();
    }

    private void smoothScrollToBottom() {
        if (binding.recyclerChat != null && mAdapter.getItemCount() > 0) {
            binding.recyclerChat.post(() ->
                    binding.recyclerChat.smoothScrollToPosition(mAdapter.getItemCount() - 1)
            );
        }
    }

    private void sendMessage(String messageText) {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> message = new HashMap<>();
        message.put("mensaje", messageText);           // "mensaje" en lugar de "message"
        message.put("emisorId", userId);              // "emisorId" en lugar de "senderId"
        message.put("timestamp", ServerValue.TIMESTAMP);
        message.put("read", false);

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .child(chatRoomId)
                .child("messages");

        chatRef.push()
                .setValue(message)
                .addOnSuccessListener(aVoid -> {
                    binding.editMessage.setText("");
                    binding.recyclerChat.scrollToPosition(mAdapter.getItemCount() - 1);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ChatActivity.this,
                                "Error al enviar mensaje: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
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

    private void loadPatientInfo() {
        if (nutriologoId == null) {
            Log.d(TAG, "nutriologoId es null en loadPatientInfo");
            return;
        }

        // Intentar cargar datos en cache primero
        String cachedInfo = prefs.getString("patient_" + nutriologoId, null);
        long lastUpdate = prefs.getLong("patient_update_" + nutriologoId, 0);

        if (cachedInfo != null && System.currentTimeMillis() - lastUpdate < CACHE_DURATION) {
            try {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> patientInfo = gson.fromJson(cachedInfo, type);
                updatePatientUI(patientInfo);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error loading cached patient info", e);
            }
        }

        // Cargar de Firestore si no hay cache o está expirado
        db.collection("patients")
                .document(nutriologoId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Map<String, String> patientInfo = new HashMap<>();
                        patientInfo.put("name", document.getString("name"));
                        patientInfo.put("selfieUrl", document.getString("selfieUrl"));
                        patientInfo.put("gender", document.getString("gender"));

                        // Guardar en cache
                        String json = gson.toJson(patientInfo);
                        prefs.edit()
                                .putString("patient_" + nutriologoId, json)
                                .putLong("patient_update_" + nutriologoId, System.currentTimeMillis())
                                .apply();

                        updatePatientUI(patientInfo);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando datos del paciente: ", e));
    }

    private void updatePatientUI(Map<String, String> patientInfo) {
        if (!isFinishing() && binding != null) {
            String name = patientInfo.get("name");
            String selfieUrl = patientInfo.get("selfieUrl");
            String gender = patientInfo.get("gender");

            if (binding.nutriologoName != null && name != null) {
                binding.nutriologoName.setText(name);
            }

            if (binding.profileImage != null) {
                if (selfieUrl != null && !selfieUrl.isEmpty()) {
                    Glide.with(ChatActivity.this)
                            .load(selfieUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(binding.profileImage);
                } else {
                    binding.profileImage.setImageResource(
                            gender != null && gender.equals("Hombre") ?
                                    R.drawable.hombre : R.drawable.mujer
                    );
                }
            }
        }
    }
}