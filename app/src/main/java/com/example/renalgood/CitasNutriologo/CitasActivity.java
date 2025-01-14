package com.example.renalgood.CitasNutriologo;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.renalgood.Nutriologo.BuzonQuejasActivity;
import com.example.renalgood.Nutriologo.NutriologoActivity;
import com.example.renalgood.Nutriologo.PacientesVinculadosActivity;
import com.example.renalgood.R;
import com.example.renalgood.mensaje.MensajeActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.annotation.NonNull;

public class CitasActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView ivHome, ivMensaje, ivCalendario, ivPacientesVinculados, ivCarta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citas);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        inicializarVistas();
        setupViewPager();
        setupNavigationListeners();
    }

    private void inicializarVistas() {
        try {
            ivHome = findViewById(R.id.ivHome);
            ivMensaje = findViewById(R.id.ivMensaje);
            ivCalendario = findViewById(R.id.ivCalendario);
            ivPacientesVinculados = findViewById(R.id.group_2811039);
            ivCarta = findViewById(R.id.ivCarta);
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar las vistas", e);
            Toast.makeText(this, "Error al inicializar la interfaz", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupNavigationListeners() {
        ivHome.setOnClickListener(view -> {
            navigateToActivity(NutriologoActivity.class);
            highlightCurrentIcon(ivHome);
        });
        ivMensaje.setOnClickListener(view -> {
            navigateToActivity(MensajeActivity.class);
            highlightCurrentIcon(ivMensaje);
        });

        ivCalendario.setImageResource(R.drawable.ic_calendar);
        ivCalendario.setColorFilter(getResources().getColor(R.color.teal_700));

        ivPacientesVinculados.setOnClickListener(view -> {
            navigateToActivity(PacientesVinculadosActivity.class);
            highlightCurrentIcon(ivPacientesVinculados);
        });
        ivCarta.setOnClickListener(view -> {
            navigateToActivity(BuzonQuejasActivity.class);
            highlightCurrentIcon(ivCarta);
        });
    }

    private void navigateToActivity(Class<?> destinationClass) {
        if (this.getClass() != destinationClass) {
            Intent intent = new Intent(this, destinationClass);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    private void highlightCurrentIcon(ImageView selectedIcon) {
        int defaultColor = ContextCompat.getColor(this, R.color.icon_default);
        int primaryColor = ContextCompat.getColor(this, R.color.primary);

        ivHome.setColorFilter(defaultColor);
        ivMensaje.setColorFilter(defaultColor);
        ivCalendario.setColorFilter(defaultColor);
        ivPacientesVinculados.setColorFilter(defaultColor);
        ivCarta.setColorFilter(defaultColor);

        selectedIcon.setColorFilter(primaryColor);
    }

    private void setupViewPager() {
        CitasPagerAdapter adapter = new CitasPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0:
                        tab.setText("Pendientes");
                        break;
                    case 1:
                        tab.setText("Confirmadas");
                        break;
                }
            }
        }).attach();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}