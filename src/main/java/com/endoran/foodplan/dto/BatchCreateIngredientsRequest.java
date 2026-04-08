package com.endoran.foodplan.dto;

import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchCreateIngredientsRequest(
        @NotEmpty @Valid List<Entry> ingredients
) {
    public record Entry(
            @NotBlank String name,
            @NotNull StorageCategory storageCategory,
            @NotNull GroceryCategory groceryCategory,
            boolean shoppingListExclude
    ) {}
}
