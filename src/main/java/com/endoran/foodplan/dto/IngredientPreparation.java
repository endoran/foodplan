package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;

import java.util.Set;

public record IngredientPreparation(
        String name,
        Status status,
        StorageCategory storageCategory,
        GroceryCategory groceryCategory,
        Set<DietaryTag> dietaryTags,
        boolean shoppingListExclude
) {
    public enum Status { EXISTING, NEW }
}
