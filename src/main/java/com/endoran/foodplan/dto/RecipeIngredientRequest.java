package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MeasurementUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
        @NotBlank String ingredientId,
        @NotBlank String ingredientName,
        @NotNull @Positive BigDecimal quantity,
        @NotNull MeasurementUnit unit
) {}
