package com.endoran.foodplan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingListResponse(
        List<ShoppingAisle> aisles,
        String storeName
) {
    public ShoppingListResponse(List<ShoppingAisle> aisles) {
        this(aisles, null);
    }
}
