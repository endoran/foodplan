package com.endoran.foodplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRecipeRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        String instructions,
        @Min(1) int baseServings,
        @Valid List<RecipeIngredientRequest> ingredients
) {}
