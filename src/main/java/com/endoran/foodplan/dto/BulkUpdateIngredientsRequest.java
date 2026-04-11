package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record BulkUpdateIngredientsRequest(
        @NotEmpty @Valid List<Entry> ingredients
) {
    public record Entry(
            @NotBlank String id,
            @NotBlank String name,
            @NotNull StorageCategory storageCategory,
            @NotNull GroceryCategory groceryCategory,
            Set<DietaryTag> dietaryTags,
            boolean shoppingListExclude
    ) {}
}
