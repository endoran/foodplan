package com.endoran.foodplan.dto;

import java.math.BigDecimal;

public record StoreProductMatch(
        String storeProductId,
        String storeAisle,
        BigDecimal storePrice,
        BigDecimal storePromoPrice,
        String storeStockLevel,
        String storeProductName,
        String storePackageSize
) {}
