package com.endoran.foodplan.service;

public class IngredientNotFoundException extends RuntimeException {

    public IngredientNotFoundException(String id) {
        super("Ingredient not found: " + id);
    }
}
