package com.endoran.foodplan.dto;

import java.math.BigDecimal;

public record ImportedIngredientPreview(
        String section,
        String name,
        BigDecimal quantity,
        String unit,
        String rawText
) {}
