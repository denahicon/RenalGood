package com.example.renalgood.historial;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorialActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String userId;
    private RecyclerView rvHistorial;
    private TextView tvWeekRange;
    private HistorialAdapter historialAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        initializeViews();
        setupFirebase();
        loadWeeklyHistory();
    }

    private void initializeViews() {
        rvHistorial = findViewById(R.id.rvHistorial);
        tvWeekRange = findViewById(R.id.tvWeekRange);

        historialAdapter = new HistorialAdapter();
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(historialAdapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    private void checkIfSundayAndLoadHistory() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.SUNDAY) {
            loadWeeklyHistory();
        } else {
            Toast.makeText(this, "El historial solo está disponible los domingos.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeeklyHistory() {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();

        tvWeekRange.setText("Semana del " + formatDate(startDate) + " al " + formatDate(endDate));

        showLoading(true);

        db.collection("usuarios")
                .document(userId)
                .collection("meals")
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HistorialActivity", "Documentos encontrados: " + queryDocumentSnapshots.size());

                    // Agrupar comidas por día
                    Map<String, DailyMealHistory> dailyMeals = new HashMap<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        MealRecord meal = document.toObject(MealRecord.class);
                        if (meal != null) {
                            String dateKey = formatDate(meal.getTimestamp());

                            DailyMealHistory dayHistory = dailyMeals.getOrDefault(dateKey,
                                    new DailyMealHistory(meal.getTimestamp(), 2000, 0, new HashMap<>(),
                                            meal.getTimestamp(), 0));

                            // Actualizar calorías consumidas
                            dayHistory.setCaloriasDiarias(
                                    dayHistory.getCaloriasDiarias() + (int)meal.getCalories());

                            // Agregar la comida al mapa de comidas del día
                            dayHistory.getMeals().put(meal.getMealType(), meal);

                            dailyMeals.put(dateKey, dayHistory);
                        }
                    }

                    List<DailyMealHistory> historyList = new ArrayList<>(dailyMeals.values());
                    Collections.sort(historyList,
                            (a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

                    historialAdapter.submitList(historyList);
                    showLoading(false);

                    showEmptyState(historyList.isEmpty());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error al cargar historial: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        View loadingView = findViewById(R.id.loadingView);
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        View emptyView = findViewById(R.id.emptyView);
        RecyclerView recyclerView = findViewById(R.id.rvHistorial);

        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, PacienteActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}