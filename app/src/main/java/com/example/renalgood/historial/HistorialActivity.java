package com.example.renalgood.historial;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private void loadWeeklyHistory() {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();

        tvWeekRange.setText("Semana del " + formatDate(startDate) + " al " + formatDate(endDate));

        db.collection("usuarios")
                .document(userId)
                .collection("historial")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HistorialActivity", "Documentos encontrados: " + queryDocumentSnapshots.size());
                    List<DailyMealHistory> historyList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d("HistorialActivity", "Procesando documento: " + document.getId());
                        DailyMealHistory history = document.toObject(DailyMealHistory.class);
                        if (history != null) {
                            historyList.add(history);
                            Log.d("HistorialActivity", "Fecha: " + history.getDate() +
                                    ", Calorías: " + history.getConsumedCalories());
                        }
                    }
                    historialAdapter.submitList(historyList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar historial: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
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