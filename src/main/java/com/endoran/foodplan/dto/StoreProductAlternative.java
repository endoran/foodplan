package com.endoran.foodplan.dto;

import java.math.BigDecimal;

public record StoreProductAlternative(
        String productId,
        String productName,
        String aisle,
        BigDecimal price,
        BigDecimal promoPrice,
        String stockLevel,
        String packageSize,
        Integer qtyNeeded,
        BigDecimal totalPrice,
        BigDecimal totalPromoPrice
) {}
