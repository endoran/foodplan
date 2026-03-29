package com.endoran.foodplan.dto;

import java.util.List;

public record ShoppingListResponse(
        List<ShoppingAisle> aisles
) {}
