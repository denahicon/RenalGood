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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecetasActivity extends AppCompatActivity implements RecetasAdapter.OnRecetaClickListener {
    private static final String TAG = "RecetasActivity";

    private RecyclerView recyclerViewRecetas;
    private TextView tvTitulo, tvHorario;
    private ProgressBar progressBar;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private FirebaseFirestore db;
    private RecetasAdapter recetasAdapter;
    private List<Recipe> recetasList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recetas);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // El usuario no está autenticado, redirigir al login
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // Guardar userId en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putString("userId", userId).apply();

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupNavigationListeners();
        loadRecetasByTime();
    }

    private void initializeViews() {
        recyclerViewRecetas = findViewById(R.id.recyclerViewRecetas);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvHorario = findViewById(R.id.tvHorario);
        progressBar = findViewById(R.id.progressBar);
        ivHome = findViewById(R.id.ivHome);
        ivLupa = findViewById(R.id.ivLupa);
        ivChef = findViewById(R.id.ivChef);
        ivMensaje = findViewById(R.id.ivMensaje);
        ivCarta = findViewById(R.id.ivCarta);
        ivCalendario = findViewById(R.id.ivCalendario);

        ivChef.setColorFilter(getResources().getColor(R.color.pink_strong));
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void setupRecyclerView() {
        recetasList = new ArrayList<>();
        String currentMealType = getCurrentMealType();
        recetasAdapter = new RecetasAdapter(this, recetasList, this, currentMealType);
        recyclerViewRecetas.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecetas.setAdapter(recetasAdapter);

        // Establecer userId inmediatamente después de crear el adaptador
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        recetasAdapter.setUserId(userId);
    }

    private void setupNavigationListeners() {
        try {
            ivHome.setOnClickListener(v -> navigateTo(PacienteActivity.class));
            ivLupa.setOnClickListener(v -> navigateTo(ListadeAlimentosActivity.class));
            ivChef.setOnClickListener(v -> navigateTo(RecetasActivity.class));
            ivMensaje.setOnClickListener(v -> navigateTo(ChatActivity.class));
            ivCarta.setOnClickListener(v -> navigateTo(BuzonQuejasPaciente.class));
            ivCalendario.setOnClickListener(v -> navigateTo(CalendarioActivity.class));
        } catch (Exception e) {
            Log.e(TAG, "Error configurando navegación: " + e.getMessage());
        }
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(this, destinationClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void loadRecetasByTime() {
        showLoading();
        String currentMealType = getCurrentMealType();
        tvHorario.setText("Recetas para " + currentMealType);

        db.collection("recipes")
                .whereEqualTo("category", currentMealType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recetasList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recetasList.add(recipe);
                        }
                    }
                    recetasAdapter.notifyDataSetChanged();
                    hideLoading();

                    if (recetasList.isEmpty()) {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Error al cargar recetas: " + e.getMessage());
                });
    }

    private String getCurrentMealType() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 11) {
            return "Desayuno";
        } else if (hour >= 11 && hour < 15) {
            return "Comida";
        } else if (hour >= 15 && hour < 18) {
            return "Merienda";
        } else {
            return "Cena";
        }
    }

    @Override
    public void onRecetaClick(Recipe recipe, ImageView imageView) {
        Intent intent = new Intent(this, RecetaDetalleActivity.class);
        intent.putExtra("recipeId", recipe.getId());
        startActivity(intent);
    }

    public void updateCaloriesProgress(int newCalories) {
        Intent intent = new Intent("UPDATE_CALORIES_PROGRESS");
        intent.putExtra("calories", newCalories);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("RecetasActivity", "Actualizando progreso de calorías: " + newCalories);
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}