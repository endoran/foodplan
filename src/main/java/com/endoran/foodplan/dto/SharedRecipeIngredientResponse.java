package com.endoran.foodplan.dto;

public record SharedRecipeIngredientResponse(
        String ingredientName,
        double quantity,
        String unit,
        String section
) {}
