package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MeasurementUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateInventoryItemRequest(
        @NotNull @Positive BigDecimal quantity,
        @NotNull MeasurementUnit unit
) {}
