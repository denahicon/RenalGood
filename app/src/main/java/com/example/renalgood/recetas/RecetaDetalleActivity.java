package com.example.renalgood.recetas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.renalgood.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Locale;

public class RecetaDetalleActivity extends AppCompatActivity {
    private static final String TAG = "RecetaDetalleActivity";
    private FirebaseFirestore db;
    private ImageView ivReceta, ivBack;
    private TextView tvNombre, tvCalorias, tvCategory;
    private LinearLayout ingredientesContainer, instruccionesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receta_detalle);

        initializeViews();
        setupFirebase();
        loadRecipeData();
    }

    private void initializeViews() {
        ivReceta = findViewById(R.id.ivReceta);
        ivBack = findViewById(R.id.ivBack);
        tvNombre = findViewById(R.id.tvNombre);
        tvCalorias = findViewById(R.id.tvCalorias);
        tvCategory = findViewById(R.id.tvCategory);
        ingredientesContainer = findViewById(R.id.ingredientesContainer);
        instruccionesContainer = findViewById(R.id.instruccionesContainer);

        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void loadRecipeData() {
        String recipeId = getIntent().getStringExtra("recipeId");
        if (recipeId != null) {
            db.collection("recipes")
                    .document(recipeId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Recipe recipe = documentSnapshot.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(documentSnapshot.getId());
                                displayRecipeDetails(recipe);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al cargar la receta", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void displayRecipeDetails(Recipe recipe) {
        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.error_recipe)
                .into(ivReceta);

        tvNombre.setText(recipe.getName());
        tvCalorias.setText(String.format(Locale.getDefault(), "%d kcal", recipe.getCalories()));
        tvCategory.setText(recipe.getCategory());

        displayIngredients(recipe.getIngredients());
        displayInstructions(recipe.getInstructions());
    }

    private void displayIngredients(List<Ingredient> ingredients) {
        ingredientesContainer.removeAllViews();
        for (Ingredient ingredient : ingredients) {
            View view = LayoutInflater.from(this).inflate(
                    R.layout.item_ingredient_detail, ingredientesContainer, false);

            TextView tvIngredient = view.findViewById(R.id.tvIngredient);
            tvIngredient.setText(String.format(Locale.getDefault(),
                    "• %s %.1f %s",
                    ingredient.getName(),
                    ingredient.getAmount(),
                    ingredient.getUnit()));

            ingredientesContainer.addView(view);
        }
    }

    private void displayInstructions(List<String> instructions) {
        instruccionesContainer.removeAllViews();
        int stepNumber = 1;
        for (String instruction : instructions) {
            View view = LayoutInflater.from(this).inflate(
                    R.layout.item_instruction_detail, instruccionesContainer, false);

            TextView tvStep = view.findViewById(R.id.tvStep);
            TextView tvInstruction = view.findViewById(R.id.tvInstruction);

            tvStep.setText(String.format(Locale.getDefault(), "Paso %d", stepNumber++));
            tvInstruction.setText(instruction);

            instruccionesContainer.addView(view);
        }
    }
}