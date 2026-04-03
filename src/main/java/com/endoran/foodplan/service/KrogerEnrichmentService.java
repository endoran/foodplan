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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KrogerEnrichmentService implements StoreEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(KrogerEnrichmentService.class);
    private static final String API_BASE = "https://api.kroger.com";

    private final String clientId;
    private final String clientSecret;
    private final String locationId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public KrogerEnrichmentService(
            @Value("${kroger.clientId:}") String clientId,
            @Value("${kroger.clientSecret:}") String clientSecret,
            @Value("${kroger.locationId:}") String locationId,
            ObjectMapper objectMapper) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.locationId = locationId;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String storeName() {
        return "Fred Meyer";
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !locationId.isBlank();
    }

    @Override
    public Map<String, StoreProductMatch> enrich(List<String> ingredientNames) {
        if (!isConfigured()) return Map.of();

        String token;
        try {
            token = getToken();
        } catch (Exception e) {
            log.warn("Kroger token fetch failed: {}", e.getMessage());
            return Map.of();
        }

        Map<String, StoreProductMatch> results = new HashMap<>();
        for (String name : ingredientNames) {
            try {
                StoreProductMatch match = searchProduct(token, name);
                if (match != null) {
                    results.put(name, match);
                }
            } catch (Exception e) {
                log.warn("Kroger lookup failed for '{}': {}", name, e.getMessage());
            }
        }
        return results;
    }

    private synchronized String getToken() throws Exception {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/v1/connect/oauth2/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scope=product.compact"))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Kroger token request returned " + response.statusCode());
        }

        JsonNode body = objectMapper.readTree(response.body());
        cachedToken = body.get("access_token").asText();
        int expiresIn = body.get("expires_in").asInt();
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
        return cachedToken;
    }

    private StoreProductMatch searchProduct(String token, String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = API_BASE + "/v1/products?filter.term=" + encoded
                + "&filter.locationId=" + locationId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Kroger product search returned {}", response.statusCode());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.get("data");
        if (data == null || data.isEmpty()) return null;

        JsonNode product = data.get(0);
        String productName = product.has("description") ? product.get("description").asText() : null;

        // Aisle
        String aisle = null;
        JsonNode aisleLocations = product.get("aisleLocations");
        if (aisleLocations != null && !aisleLocations.isEmpty()) {
            JsonNode loc = aisleLocations.get(0);
            String number = loc.has("number") ? loc.get("number").asText() : "";
            String side = loc.has("side") ? loc.get("side").asText() : "";
            aisle = (number + side).trim();
            if (aisle.isEmpty()) {
                aisle = loc.has("description") ? loc.get("description").asText() : null;
            }
        }

        // Price and stock from first item
        BigDecimal price = null;
        BigDecimal promoPrice = null;
        String stockLevel = null;
        String packageSize = null;

        JsonNode items = product.get("items");
        if (items != null && !items.isEmpty()) {
            JsonNode item = items.get(0);

            JsonNode priceNode = item.get("price");
            if (priceNode != null) {
                if (priceNode.has("regular")) price = new BigDecimal(priceNode.get("regular").asText());
                if (priceNode.has("promo") && !priceNode.get("promo").isNull() && priceNode.get("promo").asDouble() > 0) {
                    promoPrice = new BigDecimal(priceNode.get("promo").asText());
                }
            }

            JsonNode inventory = item.get("inventory");
            if (inventory != null && inventory.has("stockLevel")) {
                String raw = inventory.get("stockLevel").asText();
                stockLevel = switch (raw) {
                    case "HIGH" -> "HIGH";
                    case "LOW" -> "LOW";
                    default -> "OUT";
                };
            }

            if (item.has("size") && !item.get("size").isNull()) {
                packageSize = item.get("size").asText();
            }
        }

        return new StoreProductMatch(aisle, price, promoPrice, stockLevel, productName, packageSize);
    }
}
