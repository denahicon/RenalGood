package com.example.renalgood.recetas;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String name;
    private double amount;  // Cambiado de quantity a amount
    private String unit;

    // Constructor vacío necesario para Firestore
    public Ingredient() {}

    public Ingredient(String name, double amount, String unit) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
    }

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {  // Cambiado de getQuantity a getAmount
        return amount;
    }

    public void setAmount(double amount) {  // Cambiado de setQuantity a setAmount
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return amount + " " + unit + " " + name;
    }
}