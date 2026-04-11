package com.endoran.foodplan.dto;

import java.util.List;

public record ImportedRecipePreview(
        String name,
        String instructions,
        int baseServings,
        List<ImportedIngredientPreview> ingredients,
        String sourceUrl
) {}
