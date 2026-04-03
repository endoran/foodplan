package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.MeasurementUnit;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingItem(
        String ingredientId,
        String ingredientName,
        BigDecimal quantity,
        MeasurementUnit unit,
        String storeAisle,
        BigDecimal storePrice,
        BigDecimal storePromoPrice,
        String storeStockLevel,
        String storeProductName,
        String storePackageSize,
        Integer storeQtyNeeded
) {
    public ShoppingItem(String ingredientId, String ingredientName, BigDecimal quantity, MeasurementUnit unit) {
        this(ingredientId, ingredientName, quantity, unit, null, null, null, null, null, null, null);
    }
}
