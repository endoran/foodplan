package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;

public record IngredientPreparation(
        String name,
        Status status,
        StorageCategory storageCategory,
        GroceryCategory groceryCategory,
        boolean shoppingListExclude
) {
    public enum Status { EXISTING, NEW }
}
