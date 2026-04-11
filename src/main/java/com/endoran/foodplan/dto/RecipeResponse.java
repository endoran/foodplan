package com.endoran.foodplan.dto;

import java.util.List;

public record RecipeResponse(
        String id,
        String name,
        String instructions,
        int baseServings,
        int servings,
        List<RecipeIngredientResponse> ingredients,
        boolean shared,
        List<String> dietaryLabels
) {}
