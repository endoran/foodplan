package com.endoran.foodplan.model;

public class RecipeIngredient {

    private String ingredientId;
    private String ingredientName;
    private Measurement measurement;

    public RecipeIngredient() {
    }

    public RecipeIngredient(String ingredientId, String ingredientName, Measurement measurement) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.measurement = measurement;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }
}
