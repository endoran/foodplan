package com.endoran.foodplan.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportRecipeRequest(
        @NotBlank String url
) {}
