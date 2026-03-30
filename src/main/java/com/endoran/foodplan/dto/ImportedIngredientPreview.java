package com.endoran.foodplan.dto;

import java.math.BigDecimal;

public record ImportedIngredientPreview(
        String name,
        BigDecimal quantity,
        String unit,
        String rawText
) {}
