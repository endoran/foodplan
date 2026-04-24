package com.endoran.foodplan.dto;

import java.util.List;

public record UpdateOrgSettingsRequest(
        String timezone,
        Integer defaultServings,
        List<String> allowedRecipeSites
) {}
