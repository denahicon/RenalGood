package com.example.renalgood.recetas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.renalgood.Chat.ChatActivity;
import com.example.renalgood.ListadeAlimentos.ListadeAlimentosActivity;
import com.example.renalgood.MainActivity;
import com.example.renalgood.Paciente.BuzonQuejasPaciente;
import com.example.renalgood.agendarcitap.CalendarioActivity;
import com.example.renalgood.Paciente.PacienteActivity;
import com.example.renalgood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecetasActivity extends AppCompatActivity implements RecetasAdapter.OnRecetaClickListener {
    private static final String TAG = "RecetasActivity";
    private static final String PREFS_NAME = "RecetasPrefs";
    private static final String LAST_UPDATE_KEY = "lastUpdate";

    private RecyclerView recyclerViewRecetas;
    private TextView tvTitulo, tvHorario;
    private ProgressBar progressBar;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private FirebaseFirestore db;
    private RecetasAdapter recetasAdapter;
    private List<Recipe> recetasList;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recetas);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupNavigationListeners();
        checkDayChange();
        loadRecetasByTime();
    }

    private void initializeViews() {
        try {
            recyclerViewRecetas = findViewById(R.id.recyclerViewRecetas);
            tvTitulo = findViewById(R.id.tvTitulo);
            tvHorario = findViewById(R.id.tvHorario);
            progressBar = findViewById(R.id.progressBar);

            // Navigation icons
            ivHome = findViewById(R.id.ivHome);
            ivLupa = findViewById(R.id.ivLupa);
            ivChef = findViewById(R.id.ivChef);
            ivMensaje = findViewById(R.id.ivMensaje);
            ivCarta = findViewById(R.id.ivCarta);
            ivCalendario = findViewById(R.id.ivCalendario);

            ivChef.setColorFilter(getResources().getColor(R.color.pink_strong));
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing application", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupRecyclerView() {
        recetasList = new ArrayList<>();
        String currentMealType = getCurrentMealType();
        recetasAdapter = new RecetasAdapter(this, recetasList, this, currentMealType);

        if (recyclerViewRecetas != null) {
            recyclerViewRecetas.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewRecetas.setAdapter(recetasAdapter);
            recetasAdapter.setUserId(userId);
        }
    }

    private void setupNavigationListeners() {
        ivHome.setOnClickListener(v -> navigateTo(PacienteActivity.class));
        ivLupa.setOnClickListener(v -> navigateTo(ListadeAlimentosActivity.class));
        ivChef.setOnClickListener(v -> navigateTo(RecetasActivity.class));
        ivMensaje.setOnClickListener(v -> navigateTo(ChatActivity.class));
        ivCarta.setOnClickListener(v -> navigateTo(BuzonQuejasPaciente.class));
        ivCalendario.setOnClickListener(v -> navigateTo(CalendarioActivity.class));
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(this, destinationClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void loadRecetasByTime() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String currentMealType = getCurrentMealType();
        if (tvHorario != null) {
            tvHorario.setText("Recetas para " + currentMealType);
        }

        db.collection("recipes")
                .whereEqualTo("category", currentMealType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recetasList.clear();
                    queryDocumentSnapshots.forEach(doc -> {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recetasList.add(recipe);
                        }
                    });

                    if (recetasAdapter != null) {
                        recetasAdapter.notifyDataSetChanged();
                    }

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    if (recetasList.isEmpty()) {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Log.e(TAG, "Error loading recipes", e);
                    Toast.makeText(this, "Error al cargar recetas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getCurrentMealType() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 11) return "Desayuno";
        if (hour >= 11 && hour < 15) return "Comida";
        if (hour >= 15 && hour < 18) return "Merienda";
        return "Cena";
    }

    private void checkDayChange() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastUpdate = prefs.getLong(LAST_UPDATE_KEY, 0);
        Calendar lastUpdateCal = Calendar.getInstance();
        lastUpdateCal.setTimeInMillis(lastUpdate);

        Calendar currentCal = Calendar.getInstance();

        if (lastUpdateCal.get(Calendar.DAY_OF_YEAR) != currentCal.get(Calendar.DAY_OF_YEAR) ||
                lastUpdateCal.get(Calendar.YEAR) != currentCal.get(Calendar.YEAR)) {
            resetDailyData();
        }

        prefs.edit().putLong(LAST_UPDATE_KEY, currentCal.getTimeInMillis()).apply();
    }

    private void resetDailyData() {
        db.collection("usuarios").document(userId)
                .update("caloriasDiarias", 0)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent("UPDATE_CALORIES_PROGRESS");
                    intent.putExtra("calories", 0);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error resetting daily calories", e));
    }

    @Override
    public void onRecetaClick(Recipe recipe, ImageView imageView) {
        Intent intent = new Intent(this, RecetaDetalleActivity.class);
        intent.putExtra("recipeId", recipe.getId());
        startActivity(intent);
    }

    private void showEmptyState() {
        // Implement empty state UI
    }
}