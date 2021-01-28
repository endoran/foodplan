package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;

import java.util.Map;

public class Recipe {
    @Id
    private String id;
    private String name;
    private String instructions;
    private Map<Measurement, Ingredient> ingredients;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInstructions() {
        return instructions;
    }

    public Map<Measurement, Ingredient> getIngredients() {
        return ingredients;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setIngredients(Map<Measurement, Ingredient> ingredients) {
        this.ingredients = ingredients;
    }
}
