package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateIngredientRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotNull StorageCategory storageCategory,
        @NotNull GroceryCategory groceryCategory,
        Set<DietaryTag> dietaryTags,
        boolean shoppingListExclude
) {}
