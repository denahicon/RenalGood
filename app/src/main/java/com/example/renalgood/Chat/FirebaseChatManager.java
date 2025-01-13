package com.example.renalgood.Chat;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FirebaseChatManager {
    private static final String CHATS_REF = "chats";
    private static final String MESSAGES_REF = "messages";
    private static final String USERS_ONLINE_REF = "users_online";

    private final FirebaseFirestore db;
    private final FirebaseDatabase rtdb;
    private final String currentUserId;

    public FirebaseChatManager() {
        db = FirebaseFirestore.getInstance();
        rtdb = FirebaseDatabase.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void loadChatsForNutriologo(String nutriologoId, OnChatsLoadedListener listener) {
        db.collection("vinculaciones")
                .whereEqualTo("nutriologoId", nutriologoId)
                .whereEqualTo("estado", "activo")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<ChatPreview> chats = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String pacienteId = doc.getString("pacienteId");
                        loadChatPreview(pacienteId, chatPreview -> {
                            chats.add(chatPreview);
                            if (chats.size() == value.size()) {
                                listener.onChatsLoaded(chats);
                            }
                        });
                    }
                });
    }

    private void loadChatPreview(String participantId, OnChatPreviewLoadedListener listener) {
        String chatId = getChatId(currentUserId, participantId);

        // Cargar datos del participante
        db.collection("pacientes").document(participantId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("nombre");
                    String profilePic = doc.getString("profilePic");

                    // Cargar último mensaje
                    rtdb.getReference(CHATS_REF)
                            .child(chatId)
                            .child(MESSAGES_REF)
                            .orderByKey()
                            .limitToLast(1)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String lastMessage = "";
                                    long lastMessageTime = 0;

                                    for (DataSnapshot messageSnap : snapshot.getChildren()) {
                                        ChatMessage message = messageSnap.getValue(ChatMessage.class);
                                        if (message != null) {
                                            lastMessage = message.getMessage();
                                            lastMessageTime = message.getTimestamp();
                                        }
                                    }

                                    ChatPreview preview = new ChatPreview(
                                            chatId, participantId, name,
                                            lastMessage, lastMessageTime, profilePic
                                    );

                                    listener.onChatPreviewLoaded(preview);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    // Manejar error
                                }
                            });
                });
    }

    private String getChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    // Interfaces para callbacks
    public interface OnChatsLoadedListener {
        void onChatsLoaded(List<ChatPreview> chats);
        void onError(Exception e);
    }

    public interface OnChatPreviewLoadedListener {
        void onChatPreviewLoaded(ChatPreview chatPreview);
    }
}