package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OllamaIngredientParser {

    private static final Logger log = LoggerFactory.getLogger(OllamaIngredientParser.class);

    private final String baseUrl;
    private final String textModel;
    private final int timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a recipe ingredient parser. Your job is to convert raw ingredient strings into structured, \
            shopping-friendly data. You also enrich recipe instructions with parenthetical prep annotations.

            RULES:
            - Valid units: TSP, TBSP, CUP, PINT, QUART, GALLON, HALF_GALLON, FL_OZ, WHOLE, LBS, OZ, PINCH, PIECE, G, ML, KG, L
            - Use WHOLE for items counted by number (e.g., "3 eggs" -> quantity 3, unit WHOLE)
            - Use PINCH for "to taste", dashes, or pinches
            - COMPOUND ingredients ("salt and pepper", "oil and vinegar") -> split into separate entries sharing the same rawIndex
            - NAME = the shopping-list item. Strip ALL prep verbs: peeled, chopped, minced, sliced, diced, grated, crushed, melted, softened, julienned, trimmed, halved, quartered, seeded, deveined, shelled, cubed, cut into pieces, torn, etc.
            - PREP NOTE = preparation guidance stripped from the name. Include size/shape descriptors: "thinly sliced", "cut into 1-inch pieces", "peeled and left whole"
            - ALTERNATIVES: "X or Y flour" -> pick the first option. "Diamond Crystal or Morton" -> ignore brands, keep the ingredient
            - Unicode fractions -> decimals. Ranges -> midpoint.
            - rawIndex = 0-based index into the original ingredient list (compounds share an index)
            - INSTRUCTIONS: Where an ingredient is first used, add a parenthetical prep note if one was extracted. Example: "Add the garlic (minced) and cook..."
            - Return ONLY valid JSON. No markdown fences. No explanation.""";

    public OllamaIngredientParser(
            @Value("${ollama.host:}") String host,
            @Value("${ollama.port:11434}") int port,
            @Value("${ollama.text-model:qwen2.5:32b-instruct-q4_K_M}") String textModel,
            @Value("${ollama.timeout:120}") int timeout,
            ObjectMapper objectMapper) {
        this.baseUrl = host.isBlank() ? "" : "http://" + host + ":" + port;
        this.textModel = textModel;
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isAvailable() {
        if (baseUrl.isBlank()) return false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    public ParsedIngredientResult parseIngredients(List<String> rawIngredients, String instructions) {
        if (baseUrl.isBlank()) return null;

        try {
            String userPrompt = buildUserPrompt(rawIngredients, instructions);
            String responseJson = callOllama(userPrompt);
            ParsedIngredientResult result = parseResponse(responseJson);

            // Sanity check: discard if LLM returns >2x original ingredient count
            if (result != null && result.ingredients().size() > rawIngredients.size() * 2) {
                log.warn("LLM returned {} ingredients for {} raw inputs — discarding as likely hallucination",
                        result.ingredients().size(), rawIngredients.size());
                return null;
            }

            // Sanity check: discard if LLM returns zero ingredients
            if (result != null && result.ingredients().isEmpty()) {
                log.warn("LLM returned zero ingredients — discarding");
                return null;
            }

            return result;
        } catch (Exception e) {
            log.warn("LLM ingredient parsing failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(List<String> rawIngredients, String instructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Parse these raw ingredient strings into structured data.\n\n");
        sb.append("Raw ingredients:\n");
        for (int i = 0; i < rawIngredients.size(); i++) {
            sb.append(i).append(": \"").append(rawIngredients.get(i)).append("\"\n");
        }
        if (instructions != null && !instructions.isBlank()) {
            sb.append("\nInstructions:\n").append(instructions).append("\n");
        }
        sb.append("""

                Return JSON in this exact format:
                {
                  "ingredients": [
                    {"name": "ingredient name", "quantity": 1.0, "unit": "CUP", "section": null, "prepNote": null, "rawIndex": 0}
                  ],
                  "instructions": "1. Enriched instructions with (prep notes) added..."
                }""");
        return sb.toString();
    }

    private String callOllama(String userPrompt) throws Exception {
        Map<String, Object> systemMessage = Map.of("role", "system", "content", SYSTEM_PROMPT);
        Map<String, Object> userMessage = Map.of("role", "user", "content", userPrompt);

        Map<String, Object> body = Map.of(
                "model", textModel,
                "messages", List.of(systemMessage, userMessage),
                "temperature", 0.1,
                "max_tokens", 8192);

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(timeout))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.at("/choices/0/message/content").asText();
    }

    private ParsedIngredientResult parseResponse(String content) {
        String json = content.strip();
        // Strip markdown fences if present
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        try {
            LlmParseResponse llm = objectMapper.readValue(json, LlmParseResponse.class);

            List<ImportedIngredientPreview> ingredients = new ArrayList<>();
            for (LlmParsedIngredient ing : llm.ingredients) {
                String unit = ing.unit != null ? ing.unit.toUpperCase() : "WHOLE";
                BigDecimal qty = ing.quantity != null ? ing.quantity : BigDecimal.ONE;
                String name = ing.name != null ? ing.name.trim() : "";
                String prepNote = ing.prepNote != null && !ing.prepNote.isBlank() ? ing.prepNote.trim() : null;
                String section = ing.section != null && !ing.section.isBlank() ? ing.section.trim() : null;

                if (name.isEmpty()) continue;

                String rawText = qty.stripTrailingZeros().toPlainString() + " " + unit + " " + name;
                ingredients.add(new ImportedIngredientPreview(section, name, qty, unit, rawText, prepNote));
            }

            String enrichedInstructions = llm.instructions;
            return new ParsedIngredientResult(ingredients, enrichedInstructions);
        } catch (Exception e) {
            log.warn("Failed to parse LLM ingredient response: {}", e.getMessage());
            return null;
        }
    }

    // Result type
    public record ParsedIngredientResult(
            List<ImportedIngredientPreview> ingredients,
            String enrichedInstructions
    ) {}

    // LLM response DTOs
    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmParseResponse(
            List<LlmParsedIngredient> ingredients,
            String instructions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmParsedIngredient(
            String name,
            BigDecimal quantity,
            String unit,
            String section,
            String prepNote,
            Integer rawIndex
    ) {}
}
