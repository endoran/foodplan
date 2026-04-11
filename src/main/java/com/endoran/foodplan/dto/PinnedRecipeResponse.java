package com.endoran.foodplan.dto;

import java.time.Instant;
import java.util.List;

public record PinnedRecipeResponse(
        String id,
        String sharedRecipeId,
        String name,
        String instructions,
        int baseServings,
        List<SharedRecipeIngredientResponse> ingredients,
        String attribution,
        String sourceInstanceName,
        int pinnedVersion,
        boolean hasUpdate,
        Integer latestVersion,
        boolean sourceRemoved,
        Instant pinnedAt,
        List<String> dietaryLabels
) {}
