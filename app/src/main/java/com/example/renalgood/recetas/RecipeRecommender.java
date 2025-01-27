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

    // Límites diarios recomendados de nutrientes según diferentes condiciones clínicas.
    private static final Map<String, Map<String, Double>> DAILY_LIMITS = Map.of(
            "ERCA", Map.of(
                    "protein", 60.0,         // Límite de proteína: 0.6-0.8g/kg/día
                    "potassium", 2000.0,     // Límite de potasio: 2000-2500mg/día
                    "phosphorus", 800.0,     // Límite de fósforo: 800-1000mg/día
                    "sodium", 2000.0         // Límite de sodio: 2000-2300mg/día
            ),
            "Hemodiálisis", Map.of(
                    "protein", 75.0,         // Límite de proteína: 1.2g/kg/día
                    "potassium", 2500.0,     // Límite de potasio: 2500-3000mg/día
                    "phosphorus", 1000.0,    // Límite de fósforo: 1000-1200mg/día
                    "sodium", 2000.0         // Límite de sodio: 2000-2300mg/día
            ),
            "Diálisis peritoneal", Map.of(
                    "protein", 90.0,         // Límite de proteína: 1.2-1.3g/kg/día
                    "potassium", 3000.0,     // Potasio sin restricción estricta
                    "phosphorus", 1000.0,    // Límite de fósforo: 1000-1200mg/día
                    "sodium", 2000.0         // Límite de sodio: 2000-2300mg/día
            ),
            "Trasplante", Map.of(
                    "protein", 70.0,         // Límite de proteína: 1.0-1.2g/kg/día
                    "potassium", 2500.0,     // Límite de potasio: depende de la función renal
                    "phosphorus", 1000.0,    // Límite de fósforo: depende de la función renal
                    "sodium", 2000.0         // Límite de sodio: 2000-2300mg/día
            )
    );

    // Constructor que inicializa la instancia de Firebase Firestore.
    public RecipeRecommender() {
        this.db = FirebaseFirestore.getInstance();
    }

    //Método para obtener recetas recomendadas basadas en la condición clínica, tipo de comida y calorías objetivo.

    public void getRecommendedRecipes(String clinicalCondition, String mealType,
                                      double targetCalories, OnRecommendationsReady callback) {
        // Consulta a la colección de recetas en Firestore.
        db.collection("recipes")
                .whereEqualTo("category", mealType) // Filtra por tipo de comida.
                .whereArrayContains("suitableConditions", clinicalCondition) // Filtra por condiciones clínicas compatibles.
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> { // Éxito en la consulta.
                    Log.d(TAG, "Recetas encontradas: " + queryDocumentSnapshots.size());
                    List<Recipe> recipes = new ArrayList<>();

                    // Convierte cada documento de la consulta en un objeto Recipe.
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId()); // Asigna el ID del documento a la receta.
                            recipes.add(recipe);
                        }
                    }

                    // Filtra y puntúa las recetas basadas en los límites nutricionales.
                    List<Recipe> recommendedRecipes = filterAndScoreRecipes(
                            recipes, clinicalCondition, targetCalories);

                    // Llama al callback con las recomendaciones generadas.
                    callback.onReady(recommendedRecipes);
                })
                .addOnFailureListener(e -> callback.onError(e)); // Manejo de errores.
    }

    //este metodo
    private List<Recipe> filterAndScoreRecipes(List<Recipe> recipes,
                                               String clinicalCondition,
                                               double targetCalories) {
        // Obtiene los límites nutricionales para la condición clínica.
        Map<String, Double> limits = DAILY_LIMITS.get(clinicalCondition);
        if (limits == null) return recipes; // Si no hay límites, devuelve todas las recetas.

        // Filtra las recetas que están dentro de los límites nutricionales.
        List<Recipe> validRecipes = recipes.stream()
                .filter(recipe -> isWithinLimits(recipe, limits)) // Comprueba si cumple los límites.
                .collect(Collectors.toList());

        // Calcula y asigna una puntuación para cada receta válida.
        for (Recipe recipe : validRecipes) {
            double score = calculateRecipeScore(recipe, targetCalories, limits);
            recipe.setScore(score); // Asigna la puntuación calculada.
        }

        // Ordena las recetas por puntuación de mayor a menor.
        validRecipes.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));
        return validRecipes;
    }

    //este metodo
    private boolean isWithinLimits(Recipe recipe, Map<String, Double> limits) {
        Map<String, Double> nutrients = recipe.getNutrients(); // Nutrientes de la receta.
        // Verifica que cada nutriente no exceda un tercio del límite diario.
        return nutrients.get("protein") <= limits.get("protein") / 3 &&
                nutrients.get("potassium") <= limits.get("potassium") / 3 &&
                nutrients.get("phosphorus") <= limits.get("phosphorus") / 3 &&
                nutrients.get("sodium") <= limits.get("sodium") / 3;
    }

    private double calculateRecipeScore(Recipe recipe, double targetCalories,
                                        Map<String, Double> limits) {
        double calorieScore = calculateCalorieScore(recipe.getCalories(), targetCalories); // Puntuación por calorías.
        double nutrientScore = calculateNutrientScore(recipe.getNutrients(), limits); // Puntuación por nutrientes.

        // Promedio de las dos puntuaciones.
        return (calorieScore + nutrientScore) / 2.0;
    }

    private double calculateCalorieScore(int recipeCalories, double targetDailyCalories) {
        double targetMealCalories = targetDailyCalories / 3.0; // Calorías objetivo por comida.
        double ratio = recipeCalories / targetMealCalories; // Relación entre las calorías de la receta y las objetivo.

        // Si la relación está fuera del rango permitido, devuelve puntuación 0.
        if (ratio < 0.5 || ratio > 1.5) return 0.0;

        // Calcula la puntuación basada en la cercanía al objetivo.
        return 1.0 - Math.abs(1.0 - ratio);
    }

    //este metodo
    private double calculateNutrientScore(Map<String, Double> nutrients,
                                          Map<String, Double> limits) {
        // Calcula la puntuación individual para cada nutriente.
        double proteinScore = 1.0 - (nutrients.get("protein") * 3 / limits.get("protein"));
        double potassiumScore = 1.0 - (nutrients.get("potassium") * 3 / limits.get("potassium"));
        double phosphorusScore = 1.0 - (nutrients.get("phosphorus") * 3 / limits.get("phosphorus"));
        double sodiumScore = 1.0 - (nutrients.get("sodium") * 3 / limits.get("sodium"));

        // Promedio de las puntuaciones de los nutrientes.
        return (proteinScore + potassiumScore + phosphorusScore + sodiumScore) / 4.0;
    }

    public interface OnRecommendationsReady {
        void onReady(List<Recipe> recommendations); // Cuando las recetas están listas.
        void onError(Exception e); // Cuando ocurre un error.
    }
}
