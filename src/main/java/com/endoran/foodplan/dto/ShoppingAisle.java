package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.GroceryCategory;

import java.util.List;

public record ShoppingAisle(
        GroceryCategory category,
        List<ShoppingItem> items
) {}
