package com.endoran.foodplan.model;

import java.util.HashSet;
import java.util.Set;

public class SharedRecipeIngredient {

    private String ingredientName;
    private double quantity;
    private String unit;
    private String section;
    private Set<String> dietaryTags = new HashSet<>();
    private String ingredientId;
    private String groceryCategory;
    private String storageCategory;

    public SharedRecipeIngredient() {
    }

    public SharedRecipeIngredient(String ingredientName, double quantity, String unit, String section) {
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
        this.section = section;
    }

    public SharedRecipeIngredient(String ingredientName, double quantity, String unit, String section,
                                   Set<String> dietaryTags, String groceryCategory, String storageCategory) {
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
        this.section = section;
        this.dietaryTags = dietaryTags != null ? dietaryTags : new HashSet<>();
        this.groceryCategory = groceryCategory;
        this.storageCategory = storageCategory;
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

    public Set<String> getDietaryTags() {
        return dietaryTags;
    }

    public void setDietaryTags(Set<String> dietaryTags) {
        this.dietaryTags = dietaryTags;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getGroceryCategory() {
        return groceryCategory;
    }

    public void setGroceryCategory(String groceryCategory) {
        this.groceryCategory = groceryCategory;
    }

    public String getStorageCategory() {
        return storageCategory;
    }

    public void setStorageCategory(String storageCategory) {
        this.storageCategory = storageCategory;
    }
}
