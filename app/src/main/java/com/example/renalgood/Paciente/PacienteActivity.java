package com.example.renalgood.Paciente;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.MainActivity;
import com.example.renalgood.R;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.historial.HistorialActivity;
import com.example.renalgood.recetas.RecetasActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;

public class PacienteActivity extends AppCompatActivity {
    private static final String TAG = "PacienteActivity";

    // UI Components
    private ShapeableImageView ivImagenPaciente;
    private TextView tvNombrePaciente;
    private Chip chipEdad, chipPeso, chipAltura;
    private TextView tvCaloriasValue;
    private TextView tvSituacionClinica;
    private TextView tvGFR;
    private CircularProgressIndicator progressCalorias;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private ImageView ivNotificacion;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // Variables
    private double maxCalories;
    private NutrientCalculations calculator;
    private BroadcastReceiver caloriesUpdateReceiver;
    private boolean isActivityActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);
        isActivityActive = true;

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calculator = new NutrientCalculations();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupCaloriesReceiver();
        loadPatientData();
        setupNavigationListeners();
        checkNotificaciones();
    }

    private void initializeViews() {
        // Profile Image and Name
        ivImagenPaciente = findViewById(R.id.ivImagenPaciente);
        tvNombrePaciente = findViewById(R.id.tvNombrePaciente);

        // Chips
        chipEdad = findViewById(R.id.chipEdad);
        chipPeso = findViewById(R.id.chipPeso);
        chipAltura = findViewById(R.id.chipAltura);

        // Status and Calories
        tvSituacionClinica = findViewById(R.id.tvSituacionClinica);
        tvGFR = findViewById(R.id.tvGFR);
        tvCaloriasValue = findViewById(R.id.tvCaloriasValue);
        progressCalorias = findViewById(R.id.progressCalorias);
        progressCalorias.setMax(100);

        // Navigation Icons
        ivHome = findViewById(R.id.ivHome);
        ivLupa = findViewById(R.id.ivLupa);
        ivChef = findViewById(R.id.ivChef);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCarta = findViewById(R.id.ivCarta);
        ivCalendario = findViewById(R.id.ivCalendario);
        ivNotificacion = findViewById(R.id.ivNotificacion);

        // Setup home icon
        ivHome.setImageResource(R.drawable.ic_home);
        ivHome.setColorFilter(getResources().getColor(R.color.pink_strong));
    }

    private void loadPatientData() {
        db.collection("patients").document(userId)
                .get()
                .addOnSuccessListener(this::updateUI)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading patient data", e);
                    Toast.makeText(this, "Error cargando datos", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(DocumentSnapshot document) {
        if (!document.exists()) {
            Log.e(TAG, "Patient document does not exist");
            return;
        }

        try {
            // Basic Info
            String name = document.getString("name");
            Long age = document.getLong("age");
            Double weight = document.getDouble("weight");
            Long height = document.getLong("height");
            String clinicalSituation = document.getString("clinicalSituation");
            String gender = document.getString("gender");

            // Set Profile Image based on gender
            ivImagenPaciente.setImageResource(gender != null && gender.equals("Hombre") ?
                    R.drawable.hombre : R.drawable.mujer);

            // Update TextViews and Chips
            if (name != null) tvNombrePaciente.setText(name);
            if (age != null) chipEdad.setText(age + " años");
            if (weight != null) chipPeso.setText(weight + " kg");
            if (height != null) chipAltura.setText(height + " cm");
            if (clinicalSituation != null) tvSituacionClinica.setText("Situación: " + clinicalSituation);

            // Calculate and display GFR
            double creatinine = extractCreatinineValue(document.getString("creatinine"));
            boolean isMale = gender != null && gender.equals("Hombre");
            double gfr = calculator.calculateGFR(isMale, age != null ? age.intValue() : 0,
                    creatinine, false);
            tvGFR.setText(String.format("GFR: %.2f mL/min/1.73m²", gfr));

            // Calculate max calories
            calculateMaxCalories(document);

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    private double extractCreatinineValue(String creatinine) {
        try {
            return creatinine != null ? Double.parseDouble(creatinine.split(" ")[0]) : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }

    private void calculateMaxCalories(DocumentSnapshot document) {
        try {
            Double weight = document.getDouble("weight");
            Long height = document.getLong("height");
            Long age = document.getLong("age");
            String gender = document.getString("gender");
            String activityLevel = document.getString("daysPerWeek");

            if (weight != null && height != null && age != null && gender != null) {
                maxCalories = calculator.calculateGET(
                        gender.equals("Hombre"),
                        weight,
                        height.intValue(),
                        age.intValue(),
                        activityLevel != null ? activityLevel : "No"
                );

                loadCurrentCalories();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating calories", e);
        }
    }

    private void loadCurrentCalories() {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    Long currentCalories = userDoc.getLong("caloriasDiarias");
                    updateCaloriesProgress(currentCalories != null ? currentCalories.intValue() : 0);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading current calories", e));
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

    private void updateCaloriesProgress(int calories) {
        if (!isActivityActive) return;

        runOnUiThread(() -> {
            tvCaloriasValue.setText(String.format("%d\nkcal", calories));
            if (maxCalories > 0) {
                int progress = (int) ((calories / maxCalories) * 100);
                progressCalorias.setProgress(Math.min(progress, 100));
            }
        });
    }

    private void setupNavigationListeners() {
        // Lista de Alimentos
        ivLupa.setOnClickListener(v -> navigateTo(ListadeAlimentosActivity.class));

        // Recetas
        ivChef.setOnClickListener(v -> navigateTo(RecetasActivity.class));

        // Chat
        ivMensaje.setOnClickListener(v -> navigateTo(ChatActivity.class));

        // Buzón
        ivCarta.setOnClickListener(v -> navigateTo(BuzonQuejasPaciente.class));

        // Calendario
        ivCalendario.setOnClickListener(v -> navigateTo(CalendarioActivity.class));

        // Historial
        findViewById(R.id.btnHistorial).setOnClickListener(v -> navigateTo(HistorialActivity.class));
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(this, destinationClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void checkNotificaciones() {
        if (!isActivityActive || userId == null) return;

        db.collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("leida", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isActivityActive) return;

                    boolean hasUnreadNotifications = value != null && !value.isEmpty();
                    runOnUiThread(() -> {
                        if (ivNotificacion != null) {
                            ivNotificacion.setVisibility(hasUnreadNotifications ?
                                    View.VISIBLE : View.GONE);
                        }
                    });
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
        if (userId != null) {
            loadCurrentCalories();
            checkNotificaciones();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (caloriesUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(caloriesUpdateReceiver);
        }
    }
}