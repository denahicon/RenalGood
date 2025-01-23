package com.example.renalgood.recetas;

import static android.content.ContentValues.TAG;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecipeRecommender {
    private FirebaseFirestore db;

    private static final Map<String, Map<String, Double>> DAILY_LIMITS = Map.of(
            "ERCA", Map.of(
                    "protein", 60.0,         // 0.6-0.8g/kg/día
                    "potassium", 2000.0,     // 2000-2500mg/día
                    "phosphorus", 800.0,     // 800-1000mg/día
                    "sodium", 2000.0         // 2000-2300mg/día
            ),
            "Hemodiálisis", Map.of(
                    "protein", 75.0,         // 1.2g/kg/día
                    "potassium", 2500.0,     // 2500-3000mg/día
                    "phosphorus", 1000.0,    // 1000-1200mg/día
                    "sodium", 2000.0         // 2000-2300mg/día
            ),
            "Diálisis peritoneal", Map.of(
                    "protein", 90.0,         // 1.2-1.3g/kg/día
                    "potassium", 3000.0,     // Sin restricción estricta
                    "phosphorus", 1000.0,    // 1000-1200mg/día
                    "sodium", 2000.0         // 2000-2300mg/día
            ),
            "Trasplante", Map.of(
                    "protein", 70.0,         // 1.0-1.2g/kg/día
                    "potassium", 2500.0,     // Según función renal
                    "phosphorus", 1000.0,    // Según función renal
                    "sodium", 2000.0         // 2000-2300mg/día
            )
    );

    public RecipeRecommender() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void getRecommendedRecipes(String clinicalCondition, String mealType,
                                      double targetCalories, OnRecommendationsReady callback) {
        db.collection("recipes")
                .whereEqualTo("category", mealType)
                .whereArrayContains("suitableConditions", clinicalCondition)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Recetas encontradas: " + queryDocumentSnapshots.size());
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recipes.add(recipe);
                        }
                    }

                    List<Recipe> recommendedRecipes = filterAndScoreRecipes(
                            recipes, clinicalCondition, targetCalories);
                    callback.onReady(recommendedRecipes);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    private List<Recipe> filterAndScoreRecipes(List<Recipe> recipes,
                                               String clinicalCondition,
                                               double targetCalories) {
        Map<String, Double> limits = DAILY_LIMITS.get(clinicalCondition);
        if (limits == null) return recipes;

        List<Recipe> validRecipes = recipes.stream()
                .filter(recipe -> isWithinLimits(recipe, limits))
                .collect(Collectors.toList());

        for (Recipe recipe : validRecipes) {
            double score = calculateRecipeScore(recipe, targetCalories, limits);
            recipe.setScore(score);
        }
        validRecipes.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));
        return validRecipes;
    }

    private boolean isWithinLimits(Recipe recipe, Map<String, Double> limits) {
        Map<String, Double> nutrients = recipe.getNutrients();
        return nutrients.get("protein") <= limits.get("protein") / 3 &&
                nutrients.get("potassium") <= limits.get("potassium") / 3 &&
                nutrients.get("phosphorus") <= limits.get("phosphorus") / 3 &&
                nutrients.get("sodium") <= limits.get("sodium") / 3;
    }

    private double calculateRecipeScore(Recipe recipe, double targetCalories,
                                        Map<String, Double> limits) {
        double calorieScore = calculateCalorieScore(recipe.getCalories(), targetCalories);
        double nutrientScore = calculateNutrientScore(recipe.getNutrients(), limits);

        return (calorieScore + nutrientScore) / 2.0;
    }

    private double calculateCalorieScore(int recipeCalories, double targetDailyCalories) {
        double targetMealCalories = targetDailyCalories / 3.0;
        double ratio = recipeCalories / targetMealCalories;

        if (ratio < 0.5 || ratio > 1.5) return 0.0;
        return 1.0 - Math.abs(1.0 - ratio);
    }

    private double calculateNutrientScore(Map<String, Double> nutrients,
                                          Map<String, Double> limits) {
        double proteinScore = 1.0 - (nutrients.get("protein") * 3 / limits.get("protein"));
        double potassiumScore = 1.0 - (nutrients.get("potassium") * 3 / limits.get("potassium"));
        double phosphorusScore = 1.0 - (nutrients.get("phosphorus") * 3 / limits.get("phosphorus"));
        double sodiumScore = 1.0 - (nutrients.get("sodium") * 3 / limits.get("sodium"));

        return (proteinScore + potassiumScore + phosphorusScore + sodiumScore) / 4.0;
    }

    public interface OnRecommendationsReady {
        void onReady(List<Recipe> recommendations);
        void onError(Exception e);
    }
}