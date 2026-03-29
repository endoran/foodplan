package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MealType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateMealPlanEntryRequest(
        @NotNull LocalDate date,
        @NotNull MealType mealType,
        @NotBlank String recipeId,
        @Min(1) int servings,
        String notes
) {}
