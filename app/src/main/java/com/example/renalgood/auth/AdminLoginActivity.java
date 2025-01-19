package com.example.renalgood.auth;

import static android.content.ContentValues.TAG;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText etEmail;
    private EditText etPassword;
    private Button btnEntrar;
    private ImageButton btnBack;
    private static final int ADMIN_VERIFICATION_TIMEOUT = 10000;
    private static final String ADMIN_CACHE_KEY = "admin_verification_";
    private SharedPreferences prefs;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Inicializar Firebase usando el singleton
        firebaseManager = FirebaseManager.getInstance();

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);

        // Inicializar vistas y listeners
        initializeViews();
        setupListeners();
    }

    private void loginAdmin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Usar ValidationUtils para validaciones
        if (!ValidationUtils.validateEmail(this, etEmail)) return;
        if (!ValidationUtils.validatePassword(this, etPassword)) return;

        // Usar DialogUtils para mostrar progreso
        ProgressDialog progressDialog = DialogUtils.showProgressDialog(this, "Verificando credenciales...");

        firebaseManager.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    verificarAdmin(authResult.getUser().getUid(), progressDialog);
                })
                .addOnFailureListener(e -> {
                    DialogUtils.dismissProgressSafely(progressDialog);
                    String errorMessage = "Error de autenticación";
                    if (e instanceof FirebaseAuthException) {
                        FirebaseAuthException firebaseAuthException = (FirebaseAuthException) e;
                        errorMessage = firebaseAuthException.getErrorCode();
                    }
                    DialogUtils.showErrorDialog(this, "Error", errorMessage);
                });
    }

    private void verificarAdmin(String uid, ProgressDialog progressDialog) {
        if (uid == null || uid.isEmpty()) {
            DialogUtils.dismissProgressSafely(progressDialog);
            DialogUtils.showErrorDialog(this, "Error", "Error de autenticación");
            return;
        }

        if (prefs.getBoolean(ADMIN_CACHE_KEY + uid, false)) {
            DialogUtils.dismissProgressSafely(progressDialog);
            irAPanelAdmin();
            return;
        }

        Handler timeoutHandler = new Handler();
        Runnable timeoutRunnable = () -> {
            DialogUtils.dismissProgressSafely(progressDialog);
            DialogUtils.showErrorDialog(this, "Error", "Tiempo de espera agotado");
            firebaseManager.getAuth().signOut();
        };
        timeoutHandler.postDelayed(timeoutRunnable, ADMIN_VERIFICATION_TIMEOUT);

        firebaseManager.getDb().collection("admins").document(uid).get()
                .addOnCompleteListener(task -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    if (!task.isSuccessful()) {
                        DialogUtils.dismissProgressSafely(progressDialog);
                        handleError(task.getException());
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    DialogUtils.dismissProgressSafely(progressDialog);

                    if (document != null && document.exists()) {
                        prefs.edit().putBoolean(ADMIN_CACHE_KEY + uid, true).apply();
                        irAPanelAdmin();
                    } else {
                        showNoAdminError();
                    }
                });
    }

    private void handleError(Exception e) {
        Log.e(TAG, "Error verificando admin", e);
        DialogUtils.showErrorDialog(this, "Error",
                "Error al verificar permisos: " + e.getMessage());
        firebaseManager.getAuth().signOut();
    }

    private void showNoAdminError() {
        DialogUtils.showErrorDialog(this, "Error",
                "No tienes permisos de administrador");
        firebaseManager.getAuth().signOut();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        btnEntrar.setOnClickListener(v -> loginAdmin());

        btnBack.setOnClickListener(v -> {
            // Regresar al MainActivity
            onBackPressed();
        });
    }

    private void irAPanelAdmin() {
        Intent intent = new Intent(this, com.example.renalgood.admin.AdminActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}