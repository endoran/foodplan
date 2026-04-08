package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.StoreProductMatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scores and ranks store product matches against an ingredient name.
 * Stateless utility — all methods are static.
 */
public final class ProductMatchScorer {

    private static final Set<String> NOISE_WORDS = Set.of(
            "kroger", "fred", "meyer", "private", "selection",
            "simple", "truth", "brand", "organic", "natural",
            "store", "value", "great");

    private static final double W_NAME = 0.60;
    private static final double W_STOCK = 0.30;
    private static final double W_PRICE = 0.10;

    private ProductMatchScorer() {}

    /**
     * Rank candidates by match quality. Best match first.
     * Returns a new list (input is not modified).
     */
    public static List<StoreProductMatch> rankMatches(String ingredientName, List<StoreProductMatch> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        if (candidates.size() == 1) return List.copyOf(candidates);

        Set<String> ingredientTokens = tokenize(ingredientName);

        // Find min/max price for normalization
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        for (StoreProductMatch c : candidates) {
            BigDecimal eff = effectivePrice(c);
            if (eff == null) continue;
            if (minPrice == null || eff.compareTo(minPrice) < 0) minPrice = eff;
            if (maxPrice == null || eff.compareTo(maxPrice) > 0) maxPrice = eff;
        }

        final BigDecimal fMin = minPrice;
        final BigDecimal fMax = maxPrice;

        List<StoreProductMatch> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble((StoreProductMatch c) ->
                score(ingredientTokens, c, fMin, fMax)).reversed());
        return sorted;
    }

    static double score(Set<String> ingredientTokens, StoreProductMatch candidate,
                        BigDecimal minPrice, BigDecimal maxPrice) {
        double nameScore = nameScore(ingredientTokens, candidate.storeProductName());
        double stockScore = !"OUT".equals(candidate.storeStockLevel()) ? 1.0 : 0.0;
        double priceScore = priceScore(effectivePrice(candidate), minPrice, maxPrice);

        return W_NAME * nameScore + W_STOCK * stockScore + W_PRICE * priceScore;
    }

    static double nameScore(Set<String> ingredientTokens, String productName) {
        if (productName == null || productName.isBlank()) return 0.0;
        Set<String> productTokens = tokenize(productName);
        productTokens.removeAll(NOISE_WORDS);

        if (ingredientTokens.isEmpty() || productTokens.isEmpty()) return 0.0;

        long shared = ingredientTokens.stream().filter(productTokens::contains).count();
        return (double) shared / Math.max(ingredientTokens.size(), productTokens.size());
    }

    static double priceScore(BigDecimal price, BigDecimal min, BigDecimal max) {
        if (price == null || min == null || max == null) return 0.5;
        if (min.compareTo(max) == 0) return 1.0;
        double range = max.subtract(min).doubleValue();
        double fromMin = price.subtract(min).doubleValue();
        return 1.0 - (fromMin / range);
    }

    static BigDecimal effectivePrice(StoreProductMatch match) {
        if (match.storePromoPrice() != null) return match.storePromoPrice();
        return match.storePrice();
    }

    static Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        String[] parts = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");
        Set<String> tokens = new java.util.HashSet<>();
        for (String p : parts) {
            if (!p.isBlank()) tokens.add(p);
        }
        return tokens;
    }
}
