package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.WebRecipeSearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class WebRecipeSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebRecipeSearchService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${foodplan.ddgs.url:http://localhost:4479}")
    private String ddgsBaseUrl;

    @Value("${foodplan.web-search.enabled:false}")
    private boolean enabled;

    public List<WebRecipeSearchResult> search(String query, List<String> allowedSites) {
        if (!enabled) {
            return List.of();
        }
        if (allowedSites == null || allowedSites.isEmpty()) {
            return List.of();
        }
        String siteFilter = allowedSites.stream()
                .map(s -> "site:" + s)
                .reduce((a, b) -> a + " OR " + b)
                .orElse("");
        String fullQuery = query + " recipe (" + siteFilter + ")";
        String encoded = URLEncoder.encode(fullQuery, StandardCharsets.UTF_8);
        String url = ddgsBaseUrl + "/text?q=" + encoded + "&max_results=15";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("ddgs API returned status {}", response.statusCode());
                return List.of();
            }
            return parseResults(response.body());
        } catch (IOException | InterruptedException e) {
            log.warn("Web recipe search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<WebRecipeSearchResult> parseResults(String json) {
        try {
            List<DdgsResult> raw = MAPPER.readValue(json, new TypeReference<>() {});
            return raw.stream()
                    .filter(r -> r.href != null && !r.href.isBlank())
                    .map(r -> new WebRecipeSearchResult(
                            r.title != null ? r.title : "",
                            r.href,
                            r.body != null ? r.body : "",
                            extractSite(r.href)))
                    .filter(r -> !r.site().isEmpty())
                    .limit(20)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse ddgs response: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractSite(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "";
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "";
        }
    }

    private record DdgsResult(String title, String href, String body) {}
}
