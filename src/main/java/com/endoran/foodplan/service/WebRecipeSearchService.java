package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.WebRecipeSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebRecipeSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebRecipeSearchService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // DuckDuckGo HTML result patterns
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>(.+?)</a>",
            Pattern.DOTALL);
    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__snippet\"[^>]*>(.+?)</a>",
            Pattern.DOTALL);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    public List<WebRecipeSearchResult> search(String query, List<String> allowedSites) {
        if (allowedSites == null || allowedSites.isEmpty()) {
            return List.of();
        }
        String siteFilter = allowedSites.stream()
                .map(s -> "site:" + s)
                .reduce((a, b) -> a + " OR " + b)
                .orElse("");
        String fullQuery = query + " recipe (" + siteFilter + ")";
        String encoded = URLEncoder.encode(fullQuery, StandardCharsets.UTF_8);
        String url = "https://html.duckduckgo.com/html/?q=" + encoded;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "FoodPlan/1.0")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("DuckDuckGo returned status {}", response.statusCode());
                return List.of();
            }
            return parseResults(response.body());
        } catch (IOException | InterruptedException e) {
            log.warn("Web recipe search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<WebRecipeSearchResult> parseResults(String html) {
        List<WebRecipeSearchResult> results = new ArrayList<>();

        // Split by result blocks
        String[] blocks = html.split("class=\"result results_links");
        for (int i = 1; i < blocks.length && results.size() < 20; i++) {
            String block = blocks[i];
            Matcher linkMatcher = RESULT_PATTERN.matcher(block);
            if (!linkMatcher.find()) continue;

            String resultUrl = linkMatcher.group(1);
            String title = stripHtml(linkMatcher.group(2)).trim();

            String snippet = "";
            Matcher snippetMatcher = SNIPPET_PATTERN.matcher(block);
            if (snippetMatcher.find()) {
                snippet = stripHtml(snippetMatcher.group(1)).trim();
            }

            // Extract site domain
            String site = extractSite(resultUrl);
            if (site.isEmpty()) continue;

            results.add(new WebRecipeSearchResult(title, resultUrl, snippet, site));
        }

        return results;
    }

    private String extractSite(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "";
            // Remove www. prefix
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "";
        }
    }

    private String stripHtml(String html) {
        return HTML_TAG.matcher(html).replaceAll("")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&nbsp;", " ");
    }
}
