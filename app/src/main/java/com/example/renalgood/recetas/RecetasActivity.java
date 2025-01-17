package com.example.renalgood.recetas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class RecetasActivity extends AppCompatActivity implements RecetasAdapter.OnRecetaClickListener {
    private static final String TAG = "RecetasActivity";
    private RecyclerView recyclerViewRecetas;
    private TextView tvTitulo, tvHorario;
    private ProgressBar progressBar;
    private ImageView ivHome, ivLupa, ivChef, ivMensaje, ivCarta, ivCalendario;
    private FirebaseFirestore db;
    private RecetasAdapter recetasAdapter;
    private List<Recipe> recetasList;
    private static final String RECIPES_CACHE_KEY = "recipes_cache_";
    private static final int PAGE_SIZE = 10;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int CALORIES_UPDATE_DELAY = 500;
    private Handler caloriesHandler = new Handler();
    private Runnable caloriesUpdateRunnable;
    private SharedPreferences prefs;
    private Gson gson = new Gson();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recetas);

        prefs = getSharedPreferences("recipes_prefs", MODE_PRIVATE);
        setupRecyclerViewScrollListener();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
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
        if (isLoading) return;
        isLoading = true;
        showLoading();

        String currentMealType = getCurrentMealType();
        String cacheKey = RECIPES_CACHE_KEY + currentMealType;
        tvHorario.setText("Recetas para " + currentMealType);

        String cachedData = prefs.getString(cacheKey, null);
        if (cachedData != null) {
            try {
                List<Recipe> cachedRecipes = deserializeRecipes(cachedData);
                updateRecipesList(cachedRecipes);
                loadFromFirestore(currentMealType, true); // Cargar actualizaciones en background
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error loading cached recipes", e);
            }
        }

        loadFromFirestore(currentMealType, false);
    }

    private void loadFromFirestore(String currentMealType, boolean isBackground) {
        if (!isBackground) showLoading();

        Query query = db.collection("recipes")
                .whereEqualTo("category", currentMealType)
                .limit(PAGE_SIZE);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isBackground) {
                        recetasList.clear();
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recetasList.add(recipe);
                        }
                    }

                    // Actualizar lastVisible para paginación
                    int size = querySnapshot.size();
                    if (size > 0) {
                        lastVisible = querySnapshot.getDocuments().get(size - 1);
                    }
                    isLastPage = size < PAGE_SIZE;

                    // Guardar en cache
                    if (!isBackground) {
                        saveToCache(currentMealType, recetasList);
                    }

                    recetasAdapter.notifyDataSetChanged();
                    hideLoading();
                    isLoading = false;

                    if (recetasList.isEmpty()) {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recipes", e);
                    hideLoading();
                    isLoading = false;
                    if (!isBackground) {
                        showError("Error al cargar recetas: " + e.getMessage());
                    }
                });
    }

    private void saveToCache(String mealType, List<Recipe> recipes) {
        try {
            String serializedData = gson.toJson(recipes);
            prefs.edit()
                    .putString(RECIPES_CACHE_KEY + mealType, serializedData)
                    .putLong(RECIPES_CACHE_KEY + mealType + "_time", System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving recipes to cache", e);
        }
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

    public void updateCaloriesProgress(final int newCalories) {
        if (caloriesUpdateRunnable != null) {
            caloriesHandler.removeCallbacks(caloriesUpdateRunnable);
        }

        caloriesUpdateRunnable = () -> {
            Intent intent = new Intent("UPDATE_CALORIES_PROGRESS");
            intent.putExtra("calories", Math.max(0, newCalories));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "Actualizando progreso de calorías: " + newCalories);
        };

        caloriesHandler.postDelayed(caloriesUpdateRunnable, CALORIES_UPDATE_DELAY);
    }

    private List<Recipe> deserializeRecipes(String cachedData) {
        try {
            Type listType = new TypeToken<ArrayList<Recipe>>() {}.getType();
            List<Recipe> recipes = gson.fromJson(cachedData, listType);
            return recipes != null ? recipes : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing recipes", e);
            return new ArrayList<>();
        }
    }

    private void updateRecipesList(List<Recipe> recipes) {
        recetasList.clear();
        recetasList.addAll(recipes);
        recetasAdapter.notifyDataSetChanged();

        if (recetasList.isEmpty()) {
            showEmptyState();
        }
        hideLoading();
    }

    private void setupRecyclerViewScrollListener() {
        recyclerViewRecetas.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            loadFromFirestore(getCurrentMealType(), false);
                        }
                    }
                }
            }
        });
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