package com.example.renalgood.Nutriologo;

import android.content.Intent;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.renalgood.CitasNutriologo.CitasActivity;
import com.example.renalgood.R;
import com.example.renalgood.mensaje.MensajeActivity;

public class NavigationHelper {
    private final AppCompatActivity activity;
    private final ImageView ivHome;
    private final ImageView ivMensaje;
    private final ImageView ivCalendario;
    private final ImageView ivPacientesVinculados;
    private final ImageView ivCarta;

    public NavigationHelper(AppCompatActivity activity,
                            ImageView ivHome,
                            ImageView ivMensaje,
                            ImageView ivCalendario,
                            ImageView ivPacientesVinculados,
                            ImageView ivCarta) {
        this.activity = activity;
        this.ivHome = ivHome;
        this.ivMensaje = ivMensaje;
        this.ivCalendario = ivCalendario;
        this.ivPacientesVinculados = ivPacientesVinculados;
        this.ivCarta = ivCarta;
    }

    public void setupNavigation(String currentModule) {
        // Reset todos los iconos a su estado normal
        resetAllIcons();

        // Configura los clicks de navegación
        setupClickListeners();

        // Resalta el ícono actual según el módulo
        highlightCurrentModule(currentModule);
    }

    private void resetAllIcons() {
        int defaultColor = ContextCompat.getColor(activity, R.color.icon_default);

        ivHome.setColorFilter(defaultColor);
        ivMensaje.setColorFilter(defaultColor);
        ivCalendario.setColorFilter(defaultColor);
        ivPacientesVinculados.setColorFilter(defaultColor);
        ivCarta.setColorFilter(defaultColor);
    }

    private void setupClickListeners() {
        ivHome.setOnClickListener(v -> navigateToActivity(NutriologoActivity.class));
        ivMensaje.setOnClickListener(v -> navigateToActivity(MensajeActivity.class));
        ivCalendario.setOnClickListener(v -> navigateToActivity(CitasActivity.class));
        ivPacientesVinculados.setOnClickListener(v -> navigateToActivity(PacientesVinculadosActivity.class));
        ivCarta.setOnClickListener(v -> navigateToActivity(BuzonQuejasActivity.class));
    }

    private void highlightCurrentModule(String currentModule) {
        int selectedColor = ContextCompat.getColor(activity, R.color.teal_700);

        switch (currentModule) {
            case "home":
                ivHome.setColorFilter(selectedColor);
                break;
            case "mensaje":
                ivMensaje.setColorFilter(selectedColor);
                break;
            case "calendario":
                ivCalendario.setColorFilter(selectedColor);
                break;
            case "pacientes":
                ivPacientesVinculados.setColorFilter(selectedColor);
                break;
            case "buzon":
                ivCarta.setColorFilter(selectedColor);
                break;
        }
    }

    private void navigateToActivity(Class<?> destinationClass) {
        if (activity.getClass() != destinationClass) {
            Intent intent = new Intent(activity, destinationClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }
}