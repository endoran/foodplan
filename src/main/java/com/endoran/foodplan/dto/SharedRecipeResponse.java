package com.endoran.foodplan.dto;

import java.time.Instant;
import java.util.List;

public record SharedRecipeResponse(
        String id,
        String name,
        String instructions,
        int baseServings,
        List<SharedRecipeIngredientResponse> ingredients,
        String attribution,
        String sourceInstanceName,
        int version,
        Instant sharedAt,
        Instant updatedAt,
        boolean ownedByCurrentInstance,
        List<String> dietaryLabels
) {}
