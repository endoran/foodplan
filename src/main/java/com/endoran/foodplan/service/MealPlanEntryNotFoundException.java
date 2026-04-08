package com.endoran.foodplan.service;

public class MealPlanEntryNotFoundException extends RuntimeException {
    public MealPlanEntryNotFoundException(String id) {
        super("Meal plan entry not found: " + id);
    }
}
