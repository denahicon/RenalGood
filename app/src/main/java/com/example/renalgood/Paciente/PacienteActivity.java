package com.example.renalgood.Paciente;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
import com.google.common.reflect.TypeToken;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private boolean isActivityActive = false;
    private static final String PATIENT_CACHE_KEY = "patient_data_";
    private static final String LAST_UPDATE_KEY = "last_update";
    private static final long CACHE_DURATION = 1000 * 60 * 30;
    private SharedPreferences prefs;
    private Handler uiHandler = new Handler();
    private Runnable progressUpdateRunnable;
    private long lastCheckedTimestamp = 0;
    private static final String TAG = "PacienteActivity";
    private static final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);
        isActivityActive = true;
        prefs = getSharedPreferences("patient_prefs", MODE_PRIVATE);

        initializeViews();
        initializeFirebase();
        setupToolbar();
        loadPatientData();
        setupNavigationListeners();
        setupCaloriesReceiver();
        checkNotificaciones();
        actualizarIndicadorNotificaciones();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (caloriesUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(caloriesUpdateReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
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
        if (userId == null) return;

        String cachedData = prefs.getString(PATIENT_CACHE_KEY + userId, null);
        long lastCacheUpdate = prefs.getLong(PATIENT_CACHE_KEY + userId + "_time", 0);

        if (cachedData != null && System.currentTimeMillis() - lastCacheUpdate < CACHE_DURATION) {
            try {
                Map<String, Object> data = deserializeSnapshot(cachedData);
                updateUI(data);  // Modificar updateUI para aceptar Map<String, Object>
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error loading cached data", e);
            }
        }

        db.collection("patients")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            // Guardar en cache
                            prefs.edit()
                                    .putString(PATIENT_CACHE_KEY + userId, gson.toJson(data))
                                    .putLong(PATIENT_CACHE_KEY + userId + "_time", System.currentTimeMillis())
                                    .apply();

                            updateUI(data);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading patient data", e);
                    Toast.makeText(this, "Error cargando datos", Toast.LENGTH_SHORT).show();
                });
    }

    private String serializeSnapshot(DocumentSnapshot snapshot) {
        try {
            Map<String, Object> data = snapshot.getData();
            if (data != null) {
                return gson.toJson(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error serializing snapshot", e);
        }
        return "{}";
    }

    private Map<String, Object> deserializeSnapshot(String cached) {
        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(cached, type);
            return data != null ? data : new HashMap<>();
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing snapshot", e);
            return new HashMap<>();
        }
    }

    private void updateUI(Map<String, Object> data) {
        try {
            String name = (String) data.get("name");
            Number age = (Number) data.get("age");
            String clinicalSituation = (String) data.get("clinicalSituation");
            Number weight = (Number) data.get("weight");
            Number height = (Number) data.get("height");

            if (name != null) tvNombrePaciente.setText(name);
            if (age != null) chipEdad.setText(age.intValue() + " años");
            if (weight != null) chipPeso.setText(weight.doubleValue() + " kg");
            if (height != null) chipAltura.setText(height.intValue() + " cm");
            if (clinicalSituation != null) tvSituacionClinica.setText("Situación: " + clinicalSituation);

            // Calcular y mostrar GFR si es necesario
            calcularYMostrarGFR(data);

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    private void calcularYMostrarGFR(Map<String, Object> data) {
        try {
            String gender = (String) data.get("gender");
            Number age = (Number) data.get("age");
            String creatinine = (String) data.get("creatinine");

            if (gender != null && age != null && creatinine != null) {
                boolean isMale = gender.equals("Hombre");
                double creatinineValue = Double.parseDouble(creatinine.split(" ")[0]);
                double gfr = calculator.calculateGFR(isMale, age.intValue(), creatinineValue, false);
                tvGFR.setText(String.format("GFR: %.2f mL/min/1.73m²", gfr));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating GFR", e);
        }
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

    private void updateCaloriesProgress(final int consumedCalories) {
        // Cancelar actualización pendiente
        if (progressUpdateRunnable != null) {
            uiHandler.removeCallbacks(progressUpdateRunnable);
        }

        // Crear nueva actualización con debounce
        progressUpdateRunnable = () -> {
            if (tvCaloriasValue != null) {
                tvCaloriasValue.setText(String.format("%d\nkcal", Math.max(0, consumedCalories)));
            }

            if (progressCalorias != null && maxCalories > 0) {
                int progress = (int) (((float)Math.max(0, consumedCalories) / maxCalories) * 100);
                progressCalorias.setProgress(Math.min(progress, 100));
            }
        };

        // Ejecutar después de un pequeño delay para debouncing
        uiHandler.postDelayed(progressUpdateRunnable, 150);
    }

    private void actualizarIndicadorNotificaciones() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ImageView ivNotificacion = findViewById(R.id.ivNotificacion);

        FirebaseFirestore.getInstance()
                .collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("leida", false)
                .addSnapshotListener((value, error) -> {
                    if (!isActivityActive) return;

                    if (error != null) {
                        return;
                    }

                    runOnUiThread(() -> {
                        if (value != null && !value.isEmpty()) {
                            ivNotificacion.setVisibility(View.VISIBLE);
                        } else {
                            ivNotificacion.setVisibility(View.GONE);
                        }
                    });
                });
    }

    private void checkNotificaciones() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("notificaciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("leida", false)
                .addSnapshotListener((value, error) -> {
                    if (!isActivityActive) return; // Verificar si la actividad está activa

                    if (error != null) {
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        mostrarNotificaciones(value.getDocuments());
                    }
                });
    }

    private void checkDayChange() {
        // Evitar checks frecuentes
        if (System.currentTimeMillis() - lastCheckedTimestamp < 60000) { // 1 minuto
            return;
        }
        lastCheckedTimestamp = System.currentTimeMillis();

        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) return;

        // Verificar último update desde SharedPreferences
        long lastUpdate = prefs.getLong(LAST_UPDATE_KEY + userId, 0);
        if (lastUpdate == 0) {
            updateLastUpdate(userId);
            return;
        }

        // Verificar si es un nuevo día
        if (isNewDay(lastUpdate)) {
            resetDailyCalories(userId);
        }
    }

    private boolean isNewDay(long lastUpdate) {
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastUpdate);

        Calendar current = Calendar.getInstance();

        return last.get(Calendar.DAY_OF_YEAR) != current.get(Calendar.DAY_OF_YEAR) ||
                last.get(Calendar.YEAR) != current.get(Calendar.YEAR);
    }

    private void mostrarNotificaciones(List<DocumentSnapshot> notificaciones) {
        if (!isActivityActive) return;

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Notificaciones");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(20, 20, 20, 20);

            for (DocumentSnapshot doc : notificaciones) {
                TextView textView = new TextView(this);
                textView.setText(doc.getString("mensaje"));
                textView.setPadding(0, 10, 0, 10);
                layout.addView(textView);

                doc.getReference().update("leida", true);
            }

            builder.setView(layout);
            builder.setPositiveButton("OK", null);

            try {
                AlertDialog dialog = builder.create();
                if (!isFinishing() && !isDestroyed()) {
                    dialog.show();
                }
            } catch (Exception e) {
                Log.e("PacienteActivity", "Error mostrando notificaciones", e);
            }
        });
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

    private void resetDailyCalories(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("caloriasDiarias", 0);
        updates.put("lastUpdate", new Timestamp(new Date()));

        db.collection("usuarios")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    updateLastUpdate(userId);
                    updateCaloriesProgress(0);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error resetting calories", e));
    }

    private void updateLastUpdate(String userId) {
        prefs.edit()
                .putLong(LAST_UPDATE_KEY + userId, System.currentTimeMillis())
                .apply();
    }
}