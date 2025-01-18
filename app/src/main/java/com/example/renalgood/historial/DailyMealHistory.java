package com.example.renalgood.historial;

import java.util.Date;
import java.util.Map;

public class DailyMealHistory {
    private Date date;
    private double targetCalories;
    private double consumedCalories;
    private Map<String, MealRecord> meals;
    private Date createdAt;  // Campo createdAt
    private int caloriasDiarias;

    // Constructor vacío requerido para Firestore
    public DailyMealHistory() {}

    // Constructor completo
    public DailyMealHistory(Date date, double targetCalories, double consumedCalories,
                            Map<String, MealRecord> meals, Date createdAt, int caloriasDiarias) {
        this.date = date;
        this.targetCalories = targetCalories;
        this.consumedCalories = consumedCalories;
        this.meals = meals;
        this.createdAt = createdAt;
        this.caloriasDiarias = caloriasDiarias;
    }

    // Getters
    public Date getDate() { return date; }
    public double getTargetCalories() { return targetCalories; }
    public double getConsumedCalories() { return consumedCalories; }
    public Map<String, MealRecord> getMeals() { return meals; }
    public Date getCreatedAt() { return createdAt; }
    public int getCaloriasDiarias() { return caloriasDiarias; }

    // Setters
    public void setDate(Date date) { this.date = date; }
    public void setTargetCalories(double targetCalories) { this.targetCalories = targetCalories; }
    public void setConsumedCalories(double consumedCalories) { this.consumedCalories = consumedCalories; }
    public void setMeals(Map<String, MealRecord> meals) { this.meals = meals; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setCaloriasDiarias(int caloriasDiarias) { this.caloriasDiarias = caloriasDiarias; }
}