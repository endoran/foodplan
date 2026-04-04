package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class OllamaRecipeExtractor {

    private static final Logger log = LoggerFactory.getLogger(OllamaRecipeExtractor.class);

    private final String baseUrl;
    private final String visionModel;
    private final String textModel;
    private final int timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RecipeImportService recipeImportService;

    private static final String RECIPE_SCHEMA = """
            {
              "name": "Recipe title",
              "baseServings": 4,
              "instructions": "1. First step...\\n2. Second step...",
              "ingredients": [
                {"section": "optional section name or null", "name": "ingredient name", "quantity": 2.5, "unit": "CUP"}
              ]
            }""";

    private static final String RULES = """
            Rules:
            - Valid units: TSP, TBSP, CUP, PINT, QUART, GALLON, HALF_GALLON, FL_OZ, WHOLE, LBS, OZ, PINCH, PIECE
            - Use WHOLE for items counted by number (e.g., "3 eggs" -> quantity 3, unit WHOLE)
            - Use PINCH for "doonks", dashes, or pinches
            - Convert unicode fractions to decimals (1/2=0.5, 1/4=0.25, 3/4=0.75, 1/3=0.333, 2/3=0.667)
            - For ranges like "3 to 4 tablespoons", use the higher value
            - Group ingredients by section if the recipe has labeled sections (e.g., "Sauce", "Marinade")
            - Number each instruction step
            - Do NOT include serving suggestions, family meal ideas, or non-recipe text in instructions
            - Return ONLY valid JSON, no markdown fences or extra text""";

    private static final String VISION_PROMPT = """
            You are a recipe extraction assistant. Analyze this photo of a recipe and extract the following as JSON:
            """ + RECIPE_SCHEMA + "\n\n" + RULES;

    private static final String TEXT_PROMPT_PREFIX = """
            You are a recipe extraction assistant. The following is raw OCR text from a scanned recipe photo. \
            The OCR may contain errors, merged lines, or text from multiple columns. Extract the recipe as JSON:
            """ + RECIPE_SCHEMA + "\n\n" + RULES + """

            Additional rules for OCR cleanup:
            - Separate ingredients from instructions even if they are merged in the text
            - Fix obvious OCR errors (e.g., "cuo" -> "cup", "tep" -> "tsp", "1/2" misread as "V2")
            - Ignore page numbers, book headers/footers, and non-recipe text
            - If the recipe name seems truncated or garbled, infer it from context

            OCR Text:
            """;

    public OllamaRecipeExtractor(
            @Value("${ollama.host:}") String host,
            @Value("${ollama.port:11434}") int port,
            @Value("${ollama.vision-model:qwen3-vl:8b}") String visionModel,
            @Value("${ollama.text-model:qwen2.5:14b}") String textModel,
            @Value("${ollama.timeout:120}") int timeout,
            ObjectMapper objectMapper,
            RecipeImportService recipeImportService) {
        this.baseUrl = host.isBlank() ? "" : "http://" + host + ":" + port;
        this.visionModel = visionModel;
        this.textModel = textModel;
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.recipeImportService = recipeImportService;
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

    public ImportedRecipePreview extractFromImage(byte[] imageBytes, String mimeType) {
        if (baseUrl.isBlank()) return null;
        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String mediaType = mimeType != null ? mimeType : "image/jpeg";

            // Build OpenAI-compatible multimodal message
            Map<String, Object> imageUrl = Map.of(
                    "url", "data:" + mediaType + ";base64," + base64);
            List<Map<String, Object>> content = List.of(
                    Map.of("type", "text", "text", VISION_PROMPT),
                    Map.of("type", "image_url", "image_url", imageUrl));
            Map<String, Object> message = Map.of("role", "user", "content", content);
            Map<String, Object> body = Map.of(
                    "model", visionModel,
                    "messages", List.of(message),
                    "temperature", 0.1,
                    "max_tokens", 4096);

            String json = callOllama(body);
            return parseResponse(json);
        } catch (Exception e) {
            log.warn("Vision extraction failed: {}", e.getMessage());
            return null;
        }
    }

    public ImportedRecipePreview extractFromText(String ocrText) {
        if (baseUrl.isBlank()) return null;
        try {
            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", TEXT_PROMPT_PREFIX + ocrText);
            Map<String, Object> body = Map.of(
                    "model", textModel,
                    "messages", List.of(message),
                    "temperature", 0.1,
                    "max_tokens", 4096);

            String json = callOllama(body);
            return parseResponse(json);
        } catch (Exception e) {
            log.warn("Text extraction failed: {}", e.getMessage());
            return null;
        }
    }

    private String callOllama(Map<String, Object> body) throws Exception {
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

        // Extract content from OpenAI-format response
        JsonNode root = objectMapper.readTree(response.body());
        return root.at("/choices/0/message/content").asText();
    }

    private ImportedRecipePreview parseResponse(String content) {
        // Strip markdown fences if present
        String json = content.strip();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        LlmRecipeResponse llm;
        try {
            llm = objectMapper.readValue(json, LlmRecipeResponse.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to parse LLM response as recipe JSON", e);
        }

        List<ImportedIngredientPreview> ingredients = new ArrayList<>();
        for (LlmIngredient ing : llm.ingredients) {
            // Use existing fuzzy unit matching from RecipeImportService
            String unit = ing.unit != null ? ing.unit.toUpperCase() : "WHOLE";
            ImportedIngredientPreview parsed = recipeImportService.parseIngredientText(
                    ing.quantity + " " + unit + " " + ing.name);

            // Override with LLM values (more reliable than re-parsing)
            ingredients.add(new ImportedIngredientPreview(
                    ing.section,
                    ing.name,
                    ing.quantity != null ? ing.quantity : parsed.quantity(),
                    unit,
                    ing.quantity + " " + unit + " " + ing.name));
        }

        return new ImportedRecipePreview(
                llm.name,
                llm.instructions,
                llm.baseServings > 0 ? llm.baseServings : 1,
                ingredients,
                "scan");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmRecipeResponse(
            String name,
            int baseServings,
            String instructions,
            List<LlmIngredient> ingredients
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmIngredient(
            String section,
            String name,
            BigDecimal quantity,
            String unit
    ) {}
}
