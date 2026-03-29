package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.DietaryTag;

import java.util.Set;

public record DietaryWarning(
        String ingredientName,
        Set<DietaryTag> tags
) {}
