package com.endoran.foodplan.service;

public class RecipeNotFoundException extends RuntimeException {

    public RecipeNotFoundException(String id) {
        super("Recipe not found: " + id);
    }
}
