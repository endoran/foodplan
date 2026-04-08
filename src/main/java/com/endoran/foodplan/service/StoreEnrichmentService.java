package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.StoreProductMatch;

import java.util.List;
import java.util.Map;

public interface StoreEnrichmentService {
    Map<String, List<StoreProductMatch>> enrich(List<String> ingredientNames);
    String storeName();
}
