package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MeasurementUnit;

import java.math.BigDecimal;

public record InventoryItemResponse(
        String id,
        String ingredientId,
        String ingredientName,
        BigDecimal quantity,
        MeasurementUnit unit
) {}
