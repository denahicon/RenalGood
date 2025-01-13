package com.example.renalgood.historial;

import java.util.Date;

public class MealRecord {
    private String mealType;
    private String recipeName;
    private double calories;
    private Date timestamp;

    public MealRecord() {}

    public MealRecord(String mealType, String recipeName, double calories, Date timestamp) {
        this.mealType = mealType;
        this.recipeName = recipeName;
        this.calories = calories;
        this.timestamp = timestamp;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}