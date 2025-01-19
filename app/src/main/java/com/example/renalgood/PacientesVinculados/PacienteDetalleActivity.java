package com.example.renalgood.PacientesVinculados;

import android.content.DialogInterface;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.R;
import com.example.renalgood.historial.DailyMealHistory;
import com.example.renalgood.historial.HistorialAdapter;
import com.example.renalgood.historial.MealRecord;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PacienteDetalleActivity extends AppCompatActivity {
    private TextView tvNombre, tvEdad, tvSituacionClinica, tvPeso, tvEstatura;
    private RecyclerView rvHistorial;
    private HistorialAdapter historialAdapter;
    private FirebaseFirestore db;
    private String pacienteId;
    private List<DailyMealHistory> historialList;
    private static final String TAG = "PacienteDetalle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_detalle);

        pacienteId = getIntent().getStringExtra("pacienteId");
        if (pacienteId == null) {
            Toast.makeText(this, "Error: ID del paciente no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadPacienteInfo();
        loadHistorialSemanal();
    }

    private void initializeViews() {
        tvNombre = findViewById(R.id.tvNombre);
        tvEdad = findViewById(R.id.tvEdad);
        tvSituacionClinica = findViewById(R.id.tvSituacionClinica);
        tvPeso = findViewById(R.id.tvPeso);
        tvEstatura = findViewById(R.id.tvEstatura);
        rvHistorial = findViewById(R.id.rvHistorialAlimenticio);

        db = FirebaseFirestore.getInstance();
        historialList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        historialAdapter = new HistorialAdapter(historialList);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(historialAdapter);
    }

    private void loadHistorialSemanal() {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();

        db.collection("usuarios").document(pacienteId)
                .collection("meals")
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, DailyMealHistory> dailyMeals = new TreeMap<>();

                    Calendar tempCal = Calendar.getInstance();
                    tempCal.setTime(startDate);
                    for (int i = 0; i < 7; i++) {
                        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.getTime());
                        dailyMeals.put(dateKey, new DailyMealHistory(
                                tempCal.getTime(),
                                2000,
                                0,
                                new HashMap<>(),
                                tempCal.getTime(),
                                0
                        ));
                        tempCal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        MealRecord meal = doc.toObject(MealRecord.class);
                        if (meal != null) {
                            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(meal.getTimestamp());
                            DailyMealHistory daily = dailyMeals.get(dateKey);
                            if (daily != null) {
                                daily.getMeals().put(meal.getMealType(), meal);
                                daily.setCaloriasDiarias(daily.getCaloriasDiarias() + (int) meal.getCalories());
                            }
                        }
                    }

                    historialList.clear();
                    historialList.addAll(dailyMeals.values());
                    historialAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando historial: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar el historial", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPacienteInfo() {
        db.collection("patients")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("name");
                        Long edad = documentSnapshot.getLong("age");
                        String situacionClinica = documentSnapshot.getString("clinicalSituation");
                        Double peso = documentSnapshot.getDouble("weight");
                        Double altura = documentSnapshot.getDouble("height");
                        String actividadFisica = documentSnapshot.getString("physicalActivity");
                        String genero = documentSnapshot.getString("gender");
                        String creatinina = documentSnapshot.getString("creatinine");
                        if (nombre != null) tvNombre.setText("Nombre: " + nombre);
                        if (edad != null) tvEdad.setText("Edad: " + edad + " años");
                        if (situacionClinica != null) tvSituacionClinica.setText("Situación Clínica: " + situacionClinica);
                        if (peso != null) tvPeso.setText("Peso: " + String.format("%.1f kg", peso));
                        if (altura != null) tvEstatura.setText("Estatura: " + altura + " cm");
                        if (peso != null && altura != null) {
                            double alturaEnMetros = altura / 100.0;
                            double imc = peso / (alturaEnMetros * alturaEnMetros);
                            TextView tvIMC = findViewById(R.id.tvIMC);
                            if (tvIMC != null) {
                                tvIMC.setText(String.format("IMC: %.1f", imc));
                            }
                        }
                        TextView tvCreatinina = findViewById(R.id.tvCreatinina);
                        if (tvCreatinina != null && creatinina != null) {
                            tvCreatinina.setText("Creatinina: " + creatinina);
                        }
                        TextView tvActividad = findViewById(R.id.tvActividad);
                        if (tvActividad != null && actividadFisica != null) {
                            tvActividad.setText("Actividad Física: " + actividadFisica);
                        }

                    } else {
                        Toast.makeText(this, "No se encontró información del paciente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar datos del paciente: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar la información del paciente", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Detalle del Paciente");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_paciente_detalle, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_desvincular) {
            mostrarDialogoDesvincular();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoDesvincular() {
        new AlertDialog.Builder(this)
                .setTitle("Desvincular Paciente")
                .setMessage("¿Estás seguro que deseas desvincular a este paciente? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, desvincular", (dialog, which) -> desvincularPaciente())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void desvincularPaciente() {
        db.collection("vinculaciones")
                .whereEqualTo("pacienteId", pacienteId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().update("estado", "inactivo");
                    }

                    db.collection("patients")
                            .document(pacienteId)
                            .update("nutriologoId", null)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Paciente desvinculado exitosamente", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al actualizar paciente", e);
                                Toast.makeText(this, "Error al desvincular: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al buscar vinculación", e);
                    Toast.makeText(this, "Error al desvincular: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
