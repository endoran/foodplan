package com.endoran.foodplan.dto;

import java.util.Set;

public record SharedRecipeIngredientResponse(
        String ingredientName,
        double quantity,
        String unit,
        String section,
        Set<String> dietaryTags,
        String ingredientId,
        String groceryCategory,
        String storageCategory
) {}
