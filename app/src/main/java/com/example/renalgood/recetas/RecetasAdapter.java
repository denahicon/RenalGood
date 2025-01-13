package com.example.renalgood.recetas;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecetasAdapter extends RecyclerView.Adapter<RecetasAdapter.RecetaViewHolder> {
    private Context context;
    private List<Recipe> recetas;
    private OnRecetaClickListener listener;
    private String currentMealType;
    private FirebaseFirestore db;
    private String userId;
    private static final String TAG = "RecetasAdapter";
    private String selectedRecipeId = null;


    public interface OnRecetaClickListener {
        void onRecetaClick(Recipe recipe, ImageView imageView);
    }

    public RecetasAdapter(Context context, List<Recipe> recetas, OnRecetaClickListener listener, String currentMealType) {
        this.context = context;
        this.recetas = recetas;
        this.listener = listener;
        this.currentMealType = currentMealType;
        this.db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        this.userId = prefs.getString("userId", null);

        // Cargar selección existente al iniciar
        if (userId != null) {
            checkExistingSelection();
        }
    }

    @NonNull
    @Override
    public RecetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_receta, parent, false);
        return new RecetaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecetaViewHolder holder, int position) {
        final Recipe recipe = recetas.get(position);

        checkExistingSelection();

        holder.tvNombreReceta.setText(recipe.getName());
        holder.tvCalorias.setText(recipe.getCalories() + " kcal");

        // Cargar imagen usando Glide
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .into(holder.ivReceta);
        }

        // Restaurar estado del checkbox
        holder.checkBoxReceta.setOnCheckedChangeListener(null);
        holder.checkBoxReceta.setChecked(recipe.getId().equals(selectedRecipeId));

        holder.checkBoxReceta.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Si ya hay una receta seleccionada previamente
                if (selectedRecipeId != null) {
                    // 1. Primero reiniciamos el contador
                    resetDailyCalories();

                    // 2. Luego sumamos las calorías de la nueva receta
                    selectedRecipeId = recipe.getId();
                    updateUserCalories(recipe.getCalories(), recipe.getId());
                    updateUserMealSelection(recipe);

                    // 3. Notificamos el cambio para actualizar la UI
                    notifyDataSetChanged();
                } else {
                    // Si no hay receta seleccionada, simplemente sumamos las calorías
                    selectedRecipeId = recipe.getId();
                    updateUserCalories(recipe.getCalories(), recipe.getId());
                    updateUserMealSelection(recipe);
                }
            } else {
                // Si se deselecciona la receta actual
                if (recipe.getId().equals(selectedRecipeId)) {
                    selectedRecipeId = null;
                    resetDailyCalories(); // Reiniciamos el contador
                    removeUserMealSelection();
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecetaClick(recipe, holder.ivReceta);
            }
        });
    }

    private void resetDailyCalories() {
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("caloriasDiarias", 0);
        updates.put("lastUpdate", new Timestamp(new Date()));

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Calorías reiniciadas a 0");
                    // Actualizar la UI
                    Intent intent = new Intent("UPDATE_CALORIES_PROGRESS");
                    intent.putExtra("calories", 0);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al reiniciar calorías", e));
    }

    private void checkExistingSelection() {
        if (userId == null) return;

        db.collection("usuarios").document(userId)
                .collection("meals")
                .document(currentMealType.toLowerCase())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String existingRecipeId = documentSnapshot.getString("recipeId");
                        if (existingRecipeId != null && !existingRecipeId.equals(selectedRecipeId)) {
                            selectedRecipeId = existingRecipeId;
                            notifyDataSetChanged();
                        }
                    }
                });
    }

    private void updateUserMealSelection(Recipe recipe) {
        if (userId == null) return;

        Map<String, Object> mealData = new HashMap<>();
        mealData.put("recipeId", recipe.getId());
        mealData.put("mealType", currentMealType);
        mealData.put("calories", recipe.getCalories());
        mealData.put("timestamp", new Date());
        mealData.put("recipeName", recipe.getName());

        db.collection("usuarios").document(userId)
                .collection("meals")
                .document(currentMealType.toLowerCase())
                .set(mealData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Selección de comida guardada para " + currentMealType);
                    Toast.makeText(context,
                            "Receta seleccionada para " + currentMealType,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando selección de comida", e);
                    Toast.makeText(context,
                            "Error al guardar la selección",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void removeUserMealSelection() {
        if (userId == null) return;

        db.collection("usuarios").document(userId)
                .collection("meals")
                .document(currentMealType.toLowerCase())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Selección de comida eliminada para " + currentMealType);
                    Toast.makeText(context,
                            "Selección removida para " + currentMealType,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando selección de comida", e);
                });
    }

    @Override
    public int getItemCount() {
        return recetas.size();
    }

    private void updateUserCalories(int calories, String recipeId) {
        if (userId == null) {
            Log.e(TAG, "UserId es null");
            return;
        }

        final DocumentReference userRef = db.collection("usuarios").document(userId);  // Cambiado a usuarios
        final int caloriesChange = calories;

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        updateExistingUserCalories(userRef, documentSnapshot, caloriesChange);
                    } else {
                        createNewUserDocument(userRef, caloriesChange);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener documento de usuario: " + e.getMessage());
                    if (context != null) {
                        Toast.makeText(context, "Error al actualizar calorías",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateExistingUserCalories(final DocumentReference userRef,
                                            DocumentSnapshot documentSnapshot,
                                            final int caloriesChange) {
        // Verificar si necesitamos reiniciar las calorías
        Timestamp lastUpdate = documentSnapshot.getTimestamp("lastUpdate");
        Calendar lastUpdateCal = Calendar.getInstance();
        Calendar currentCal = Calendar.getInstance();

        if (lastUpdate != null) {
            lastUpdateCal.setTime(lastUpdate.toDate());
        }

        boolean shouldReset = lastUpdate == null ||
                lastUpdateCal.get(Calendar.DAY_OF_YEAR) != currentCal.get(Calendar.DAY_OF_YEAR) ||
                lastUpdateCal.get(Calendar.YEAR) != currentCal.get(Calendar.YEAR);

        int currentCalories;
        if (shouldReset) {
            // Si es un nuevo día, empezar desde 0
            currentCalories = 0;
        } else {
            // Si es el mismo día, usar las calorías actuales
            Long currentCaloriesLong = documentSnapshot.getLong("caloriasDiarias");
            currentCalories = currentCaloriesLong != null ? currentCaloriesLong.intValue() : 0;
        }

        final int newCalories = Math.max(0, currentCalories + caloriesChange);

        Map<String, Object> updates = new HashMap<>();
        updates.put("caloriasDiarias", newCalories);
        updates.put("lastUpdate", new Timestamp(new Date()));

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Calorías actualizadas: " + newCalories);
                    // Enviar broadcast para actualizar PacienteActivity
                    Intent intent = new Intent("UPDATE_CALORIES_PROGRESS");
                    intent.putExtra("calories", newCalories);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar calorías: " + e.getMessage());
                    if (context != null) {
                        Toast.makeText(context, "Error al actualizar calorías",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewUserDocument(final DocumentReference userRef, final int initialCalories) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("caloriasDiarias", Math.max(0, initialCalories));
        userData.put("createdAt", new Timestamp(new Date()));
        userData.put("lastUpdate", new Timestamp(new Date()));

        userRef.set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Documento de usuario creado con calorías iniciales: " + initialCalories);
                    Intent intent = new Intent("UPDATE_CALORIES_PROGRESS");
                    intent.putExtra("calories", initialCalories);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al crear documento de usuario: " + e.getMessage());
                    if (context != null) {
                        Toast.makeText(context, "Error al crear perfil de usuario",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void setUserId(String userId) {
        this.userId = userId;
        Log.d(TAG, "UserId actualizado: " + userId);
    }

    static class RecetaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReceta;
        TextView tvNombreReceta;
        TextView tvCalorias;
        CheckBox checkBoxReceta;

        RecetaViewHolder(View itemView) {
            super(itemView);
            ivReceta = itemView.findViewById(R.id.ivReceta);
            tvNombreReceta = itemView.findViewById(R.id.tvNombreReceta);
            tvCalorias = itemView.findViewById(R.id.tvCalorias);
            checkBoxReceta = itemView.findViewById(R.id.checkBoxReceta);
        }
    }
}