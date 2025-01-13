package com.example.renalgood.historial;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class DailyMealHistory {
    private Date date;
    private double targetCalories;
    private double consumedCalories;
    private Map<String, MealRecord> meals;

    public DailyMealHistory() {}

    public DailyMealHistory(Date date, double targetCalories, double consumedCalories, Map<String, MealRecord> meals) {
        this.date = date;
        this.targetCalories = targetCalories;
        this.consumedCalories = consumedCalories;
        this.meals = meals;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(double targetCalories) {
        this.targetCalories = targetCalories;
    }

    public double getConsumedCalories() {
        return consumedCalories;
    }

    public void setConsumedCalories(double consumedCalories) {
        this.consumedCalories = consumedCalories;
    }

    public Map<String, MealRecord> getMeals() {
        return meals;
    }

    public void setMeals(Map<String, MealRecord> meals) {
        this.meals = meals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DailyMealHistory that = (DailyMealHistory) o;

        if (Double.compare(that.targetCalories, targetCalories) != 0) return false;
        if (Double.compare(that.consumedCalories, consumedCalories) != 0) return false;
        if (!Objects.equals(date, that.date)) return false;
        return Objects.equals(meals, that.meals);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = date.hashCode();
        temp = Double.doubleToLongBits(targetCalories);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(consumedCalories);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + meals.hashCode();
        return result;
    }
}