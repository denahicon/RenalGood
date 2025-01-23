package com.example.renalgood.recetas;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Recipe implements Serializable {
    private String id;
    private String name;
    private String category;
    private int calories;
    private String imageUrl;
    private double score;
    private List<Ingredient> ingredients;
    private List<String> instructions;
    private Map<String, Double> nutrients;
    private Timestamp timestamp;
    public Recipe() {}

    // Constructor completo
    public Recipe(String id, String name, String category, int calories,
                  String imageUrl, List<Ingredient> ingredients,
                  List<String> instructions, Map<String, Double> nutrients,
                  Timestamp timestamp) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.calories = calories;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.nutrients = nutrients;
        this.timestamp = timestamp;
    }

    // Constructor básico para compatibilidad con versión anterior
    public Recipe(String id, String name, String category, int calories, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.calories = calories;
        this.imageUrl = imageUrl;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
    }

    public Map<String, Double> getNutrients() {
        return nutrients;
    }

    public void setNutrients(Map<String, Double> nutrients) {
        this.nutrients = nutrients;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}