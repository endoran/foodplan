package com.endoran.foodplan.dto;

import java.util.List;

public record OrgSettingsResponse(
        String timezone,
        int defaultServings,
        List<String> allowedRecipeSites,
        List<String> defaultRecipeSites
) {}
