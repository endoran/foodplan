package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MealStatus;
import com.endoran.foodplan.model.MealType;

import java.time.LocalDate;
import java.util.List;

public record MealPlanEntryResponse(
        String id,
        LocalDate date,
        MealType mealType,
        String recipeId,
        String recipeName,
        int servings,
        String notes,
        MealStatus status,
        List<DietaryWarning> warnings
) {}
