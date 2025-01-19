package com.example.renalgood.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseAuthHandler {
    private static final String TAG = "FirebaseAuthHandler";
    private static final String CACHE_PREFIX = "user_email_";
    private static final long CACHE_DURATION = 1000 * 60 * 30;
    private final SharedPreferences prefs;
    private final Context context;
    private final FirebaseManager firebaseManager;

    public interface OnPasswordResetComplete {
        void onComplete(boolean success, String message);
    }

    public interface OnUserExistsCheck {
        void onComplete(boolean exists);
    }

    public interface OnStatusUpdate {
        void onComplete(boolean success);
    }

    public FirebaseAuthHandler(Context context) {
        this.context = context;
        this.firebaseManager = FirebaseManager.getInstance();
        this.prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);

        cleanupOldCache();
    }

    public void handlePasswordReset(String email, OnPasswordResetComplete callback) {
        checkUserExists(email, exists -> {
            if (!exists) {
                callback.onComplete(false, "No se encontró ninguna cuenta con este correo electrónico");
                return;
            }

            firebaseManager.getAuth().sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> updatePasswordResetStatus(email, true, updateSuccess -> {
                        if (updateSuccess) {
                            callback.onComplete(true, null);
                        } else {
                            callback.onComplete(false, "Error actualizando el estado de recuperación");
                        }
                    }))
                    .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
        });
    }

    private void checkUserExists(String email, OnUserExistsCheck callback) {
        if (email == null || email.isEmpty()) {
            callback.onComplete(false);
            return;
        }

        String cacheKey = CACHE_PREFIX + email.toLowerCase();
        long lastCheck = prefs.getLong(cacheKey + "_time", 0);
        boolean cachedResult = prefs.getBoolean(cacheKey + "_exists", false);

        if (System.currentTimeMillis() - lastCheck < CACHE_DURATION) {
            Log.d(TAG, "Using cached result for email check: " + email);
            callback.onComplete(cachedResult);
            return;
        }

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        tasks.add(firebaseManager.getDb().collection("patients").whereEqualTo("email", email).limit(1).get());
        tasks.add(firebaseManager.getDb().collection("doctors").whereEqualTo("email", email).limit(1).get());

        Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
            boolean exists = false;
            for (Task<QuerySnapshot> subTask : tasks) {
                if (subTask.isSuccessful() && !subTask.getResult().isEmpty()) {
                    exists = true;
                    break;
                }
            }

            prefs.edit()
                    .putBoolean(cacheKey + "_exists", exists)
                    .putLong(cacheKey + "_time", System.currentTimeMillis())
                    .apply();

            callback.onComplete(exists);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking user existence", e);
            callback.onComplete(false);
        });
    }

    private void updatePasswordResetStatus(String email, boolean resetPending, OnStatusUpdate callback) {
        updateCollectionResetStatus("patients", email, resetPending, success -> {
            if (success) {
                callback.onComplete(true);
            } else {
                updateCollectionResetStatus("doctors", email, resetPending, callback);
            }
        });
    }

    private void updateCollectionResetStatus(String collection, String email, boolean resetPending, OnStatusUpdate callback) {
        firebaseManager.getDb().collection(collection).whereEqualTo("email", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().get(0).getReference()
                                .update("passwordResetPending", resetPending,
                                        "lastPasswordReset", com.google.firebase.Timestamp.now())
                                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                                .addOnFailureListener(e -> callback.onComplete(false));
                    } else {
                        callback.onComplete(false);
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    private void cleanupOldCache() {
        Map<String, ?> allPrefs = prefs.getAll();
        long currentTime = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(CACHE_PREFIX) && key.endsWith("_time")) {
                long timestamp = (long) entry.getValue();
                if (currentTime - timestamp > CACHE_DURATION) {
                    String baseKey = key.substring(0, key.length() - 5); // Eliminar "_time"
                    editor.remove(key);
                    editor.remove(baseKey + "_exists");
                }
            }
        }

        editor.apply();
    }

}