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
        authHandler = new FirebaseAuthHandler(this);
        progressDialog = DialogUtils.showProgressDialog(this, "Enviando correo de recuperación...");
        progressDialog.dismiss(); // Lo ocultamos inicialmente
    }

    private void setupListeners() {
        binding.enviar.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            validateAndSendEmail(email);
        });
        binding.volverLogin.setOnClickListener(v -> finish());
    }

    private void validateAndSendEmail(String email) {
        binding.email.setError(null);
        if (!ValidationUtils.validateEmail(this, binding.email)) {
            return;
        }
        sendPasswordResetEmail(email);
    }

    private void sendPasswordResetEmail(String email) {
        progressDialog.show();

        authHandler.handlePasswordReset(email, new FirebaseAuthHandler.OnPasswordResetComplete() {
            @Override
            public void onComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    DialogUtils.dismissProgressSafely(progressDialog);
                    if (success) {
                        DialogUtils.showSuccessDialog(RecuperarContrasenaActivity.this,
                                "¡Correo enviado!",
                                "Hemos enviado las instrucciones de recuperación a tu correo electrónico. " +
                                        "Por favor revisa tu bandeja de entrada y sigue los pasos indicados.",
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    finish();
                                });
                    } else {
                        DialogUtils.showErrorDialog(RecuperarContrasenaActivity.this,
                                "Error",
                                "No pudimos enviar el correo de recuperación: " + message);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DialogUtils.dismissProgressSafely(progressDialog);
    }
}