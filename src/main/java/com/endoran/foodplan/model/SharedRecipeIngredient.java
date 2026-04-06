package com.endoran.foodplan.model;

public class SharedRecipeIngredient {

    private String ingredientName;
    private double quantity;
    private String unit;
    private String section;

    public SharedRecipeIngredient() {
    }

    public SharedRecipeIngredient(String ingredientName, double quantity, String unit, String section) {
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
        this.section = section;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
