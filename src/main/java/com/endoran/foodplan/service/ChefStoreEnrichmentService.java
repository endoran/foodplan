package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.StoreProductMatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChefStoreEnrichmentService implements StoreEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(ChefStoreEnrichmentService.class);

    private final String appId;
    private final String apiKey;
    private final String storeNumber;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChefStoreEnrichmentService(
            @Value("${chefstore.algolia.appId:70KQ5FEQ31}") String appId,
            @Value("${chefstore.algolia.apiKey:48035a5322b28e6485ae9f3b235b150a}") String apiKey,
            @Value("${chefstore.algolia.storeNumber:553}") String storeNumber,
            ObjectMapper objectMapper) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.storeNumber = storeNumber;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String storeName() {
        return "Cash 'n Carry";
    }

    @Override
    public Map<String, StoreProductMatch> enrich(List<String> ingredientNames) {
        Map<String, StoreProductMatch> results = new HashMap<>();
        for (String name : ingredientNames) {
            try {
                StoreProductMatch match = searchProduct(name);
                if (match != null) {
                    results.put(name, match);
                }
            } catch (Exception e) {
                log.warn("CHEF'STORE lookup failed for '{}': {}", name, e.getMessage());
            }
        }
        return results;
    }

    private StoreProductMatch searchProduct(String query) throws Exception {
        String endpoint = "https://" + appId + "-dsn.algolia.net/1/indexes/prod_centralia_products/query";
        String body = objectMapper.writeValueAsString(Map.of(
                "query", query,
                "hitsPerPage", 3
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("X-Algolia-Application-Id", appId)
                .header("X-Algolia-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("CHEF'STORE Algolia returned {}", response.statusCode());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode hits = root.get("hits");
        if (hits == null || hits.isEmpty()) return null;

        for (JsonNode hit : hits) {
            String exists = getStoreField(hit, "itemExists");
            if (!"1".equals(exists)) continue;

            String productName = hit.has("itemDesc1") ? hit.get("itemDesc1").asText() : null;
            String priceStr = getStoreField(hit, "unitSellPrice");
            String regPriceStr = getStoreField(hit, "unitRegPrice");
            String area = getStoreField(hit, "area");
            String lowStock = getStoreField(hit, "lowStock");

            BigDecimal price = parsePrice(priceStr);
            BigDecimal regPrice = parsePrice(regPriceStr);

            // If sell price differs from regular price, regular is the "promo" baseline
            BigDecimal promoPrice = null;
            if (price != null && regPrice != null && price.compareTo(regPrice) < 0) {
                promoPrice = price;
                price = regPrice;
            }

            String stockLevel = "HIGH";
            if ("1".equals(lowStock)) stockLevel = "LOW";

            return new StoreProductMatch(area, price, promoPrice, stockLevel, productName);
        }

        return null;
    }

    private String getStoreField(JsonNode hit, String fieldName) {
        JsonNode field = hit.get(fieldName);
        if (field == null) return null;
        if (field.isObject() && field.has(storeNumber)) {
            return field.get(storeNumber).asText();
        }
        if (field.isTextual()) return field.asText();
        return null;
    }

    private BigDecimal parsePrice(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
