package com.endoran.foodplan.dto;

import java.util.List;

public record ScanResult(String scanSessionId, List<ImportedRecipePreview> recipes) {}
