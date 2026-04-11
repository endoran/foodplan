package com.endoran.foodplan.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PrepareIngredientsRequest(
        @NotEmpty List<String> ingredientNames
) {}
