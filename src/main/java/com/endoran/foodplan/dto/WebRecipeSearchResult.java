package com.endoran.foodplan.dto;

public record WebRecipeSearchResult(
        String title,
        String url,
        String snippet,
        String site
) {}
