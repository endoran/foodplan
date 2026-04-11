package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MeasurementUnit;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingItem(
        String ingredientId,
        String ingredientName,
        BigDecimal quantity,
        MeasurementUnit unit,
        List<StoreProductAlternative> storeProducts
) {
    public ShoppingItem(String ingredientId, String ingredientName, BigDecimal quantity, MeasurementUnit unit) {
        this(ingredientId, ingredientName, quantity, unit, null);
    }
}
