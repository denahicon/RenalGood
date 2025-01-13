package com.example.renalgood.auth;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.renalgood.databinding.ActivityRecuperarContrasenaBinding;

public class RecuperarContrasenaActivity extends AppCompatActivity {

    private ActivityRecuperarContrasenaBinding binding;
    private FirebaseAuthHandler authHandler;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecuperarContrasenaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initComponents();
        setupListeners();
    }

    private void initComponents() {
        // Inicializar el manejador de autenticación
        authHandler = new FirebaseAuthHandler(this);

        // Configurar el diálogo de progreso
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando correo de recuperación...");
        progressDialog.setCancelable(false);
    }

    private void setupListeners() {
        // Listener para el botón de enviar
        binding.enviar.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            validateAndSendEmail(email);
        });

        // Listener para el botón de volver al login
        binding.volverLogin.setOnClickListener(v -> finish());
    }

    private void validateAndSendEmail(String email) {
        // Limpiar error previo si existe
        binding.email.setError(null);

        // Validar email
        if (email.isEmpty()) {
            binding.email.setError("Por favor ingresa tu correo electrónico");
            binding.email.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            binding.email.setError("Por favor ingresa un correo electrónico válido");
            binding.email.requestFocus();
            return;
        }

        // Si pasa las validaciones, enviar el correo
        sendPasswordResetEmail(email);
    }

    private void sendPasswordResetEmail(String email) {
        progressDialog.show();

        authHandler.handlePasswordReset(email, new FirebaseAuthHandler.OnPasswordResetComplete() {
            @Override
            public void onComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (success) {
                        showSuccessDialog();
                    } else {
                        showErrorDialog(message);
                    }
                });
            }
        });
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("¡Correo enviado!")
                .setMessage("Hemos enviado las instrucciones de recuperación a tu correo electrónico. " +
                        "Por favor revisa tu bandeja de entrada y sigue los pasos indicados.")
                .setPositiveButton("Entendido", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog(String errorMessage) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Error")
                .setMessage("No pudimos enviar el correo de recuperación: " + errorMessage)
                .setPositiveButton("Intentar de nuevo", null)
                .show();
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}