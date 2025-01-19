package com.example.renalgood.auth;

import android.content.Context;
import android.util.Patterns;
import android.widget.EditText;

public class ValidationUtils {
    public static boolean validateEmail(Context context, EditText etEmail) {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Ingrese el correo");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingrese un correo válido");
            etEmail.requestFocus();
            return false;
        }

        return true;
    }

    public static boolean validatePassword(Context context, EditText etPassword) {
        String password = etPassword.getText().toString().trim();

        if (password.isEmpty()) {
            etPassword.setError("Ingrese la contraseña");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    public static boolean validateNumberInput(EditText input, String fieldName, double min, double max) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            input.setError(fieldName + " es requerido");
            return false;
        }

        try {
            double num = Double.parseDouble(value);
            if (num < min || num > max) {
                input.setError(fieldName + " debe estar entre " + min + " y " + max);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            input.setError("Número inválido");
            return false;
        }
    }
}