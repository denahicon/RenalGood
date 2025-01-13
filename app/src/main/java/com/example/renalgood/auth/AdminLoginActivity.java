package com.example.renalgood.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText etEmail;
    private EditText etPassword;
    private Button btnEntrar;
    private ImageButton btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        initializeViews();

        // Configurar listeners
        setupListeners();
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

    private void loginAdmin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validaciones
        if (email.isEmpty()) {
            etEmail.setError("Ingrese el correo");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingrese un correo válido");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Ingrese la contraseña");
            etPassword.requestFocus();
            return;
        }

        // Mostrar progreso
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verificando credenciales...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    verificarAdmin(authResult.getUser().getUid(), progressDialog);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    String errorMessage = "Error de autenticación";
                    if (e instanceof FirebaseAuthException) {
                        FirebaseAuthException firebaseAuthException = (FirebaseAuthException) e;
                        errorMessage = firebaseAuthException.getErrorCode();
                    }
                    Toast.makeText(AdminLoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
    }

    private void verificarAdmin(String uid, ProgressDialog progressDialog) {
        db.collection("admins")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressDialog.dismiss();
                    if (documentSnapshot.exists()) {
                        // Es admin, ir al panel de administrador
                        irAPanelAdmin();
                    } else {
                        // No es admin
                        Toast.makeText(this,
                                "No tienes permisos de administrador",
                                Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this,
                            "Error al verificar permisos: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                });
    }

    private void irAPanelAdmin() {
        Intent intent = new Intent(this, com.example.renalgood.admin.AdminActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}