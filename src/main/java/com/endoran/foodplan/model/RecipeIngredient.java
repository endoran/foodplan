package com.endoran.foodplan.model;

public class RecipeIngredient {

    private String section;
    private String ingredientId;
    private String ingredientName;
    private Measurement measurement;

    public RecipeIngredient() {
    }

    public RecipeIngredient(String ingredientId, String ingredientName, Measurement measurement) {
        this(null, ingredientId, ingredientName, measurement);
    }

    public RecipeIngredient(String section, String ingredientId, String ingredientName, Measurement measurement) {
        this.section = section;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.measurement = measurement;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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
