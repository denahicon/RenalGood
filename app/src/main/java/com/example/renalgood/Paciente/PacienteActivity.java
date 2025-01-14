package com.example.renalgood.Paciente;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.historial.HistorialActivity;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.R;
import com.example.renalgood.recetas.RecetasActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class PacienteActivity extends AppCompatActivity {

    private ShapeableImageView ivImagenPaciente;
    private TextView tvNombrePaciente, tvCaloriasValue, tvSituacionClinica, tvGFR;
    private Chip chipEdad, chipPeso, chipAltura;
    private CircularProgressIndicator progressCalorias;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private BroadcastReceiver caloriesUpdateReceiver;
    private double maxCalories;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NutrientCalculations calculator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);

        initializeViews();
        initializeFirebase();
        setupToolbar();
        loadPatientData();
        setupNavigationListeners();
        setupCaloriesReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (caloriesUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(caloriesUpdateReceiver);
        }
    }

    private void initializeViews() {
        ivImagenPaciente = findViewById(R.id.ivImagenPaciente);
        tvNombrePaciente = findViewById(R.id.tvNombrePaciente);
        chipEdad = findViewById(R.id.chipEdad);
        chipPeso = findViewById(R.id.chipPeso);
        chipAltura = findViewById(R.id.chipAltura);
        tvCaloriasValue = findViewById(R.id.tvCaloriasValue);
        tvSituacionClinica = findViewById(R.id.tvSituacionClinica);
        tvGFR = findViewById(R.id.tvGFR);
        progressCalorias = findViewById(R.id.progressCalorias);
        ivHome = findViewById(R.id.ivHome);
        ivLupa = findViewById(R.id.ivLupa);
        ivChef = findViewById(R.id.ivChef);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCarta = findViewById(R.id.ivCarta);
        ivCalendario = findViewById(R.id.ivCalendario);

        // Configurar progressBar
        progressCalorias.setMax(100);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calculator = new NutrientCalculations();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void loadPatientData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("patients").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        updateUI(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar error de carga de datos
                });
    }

    private void updateUI(DocumentSnapshot document) {
        String gender = document.getString("gender");
        ivImagenPaciente.setImageResource(gender != null && gender.equals("Hombre") ? R.drawable.hombre : R.drawable.mujer);
        tvNombrePaciente.setText(document.getString("name"));
        chipEdad.setText(document.getLong("age") + " años");
        chipPeso.setText(document.getDouble("weight") + " kg");
        chipAltura.setText(document.getLong("height") + " cm");

        // Calcular y mostrar GFR
        double creatinine = extractCreatinineValue(document.getString("creatinine"));
        boolean isMale = gender != null && gender.equals("Hombre");
        int age = document.getLong("age").intValue();
        double gfr = calculator.calculateGFR(isMale, age, creatinine, false);

        tvSituacionClinica.setText("Situación: " + document.getString("clinicalSituation"));
        tvGFR.setText(String.format("GFR: %.2f mL/min/1.73m²", gfr));

        // Calcular y mostrar calorías
        calculateAndShowCalories(document);
    }

    private void calculateAndShowCalories(DocumentSnapshot document) {
        double weight = document.getDouble("weight");
        int height = document.getLong("height").intValue();
        int age = document.getLong("age").intValue();
        String gender = document.getString("gender");
        String activityLevel = document.getString("daysPerWeek");

        maxCalories = calculator.calculateGET(gender.equals("Hombre"), weight, height, age, activityLevel);

        // Obtener las calorías consumidas del día
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    int consumedCalories = userDoc.getLong("caloriasDiarias") != null ?
                            userDoc.getLong("caloriasDiarias").intValue() : 0;
                    updateCaloriesProgress(consumedCalories);
                });
    }

    private double extractCreatinineValue(String creatinine) {
        try {
            return Double.parseDouble(creatinine.split(" ")[0]);
        } catch (Exception e) {
            return 1.0; // valor por defecto en caso de error
        }
    }

    private void setupCaloriesReceiver() {
        caloriesUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int newCalories = intent.getIntExtra("calories", 0);
                updateCaloriesProgress(newCalories);
            }
        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(caloriesUpdateReceiver, new IntentFilter("UPDATE_CALORIES_PROGRESS"));
    }

    private void updateCaloriesProgress(int consumedCalories) {
        tvCaloriasValue.setText(String.format("%d\nkcal", consumedCalories));
        int progress = (int) ((consumedCalories / maxCalories) * 100);
        progressCalorias.setProgress(Math.min(progress, 100));
    }


    private void setupNavigationListeners() {
        ivHome.setImageResource(R.drawable.ic_home);
        ivHome.setColorFilter(getResources().getColor(R.color.pink_strong));

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

        ivMensaje.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

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

        findViewById(R.id.btnHistorial).setOnClickListener(v -> {
            Intent intent = new Intent(this, HistorialActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }
}