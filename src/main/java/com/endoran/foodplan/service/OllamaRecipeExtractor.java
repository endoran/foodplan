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
              "recipes": [
                {
                  "name": "Recipe title",
                  "baseServings": 4,
                  "instructions": "1. First step...\\n2. Second step...",
                  "ingredients": [
                    {"section": "optional section name or null", "name": "ingredient name", "quantity": 2.5, "unit": "CUP", "prepNote": "optional prep note or null"}
                  ]
                }
              ]
            }""";

    private static final String RULES = """
            Rules:
            - This image/text may contain ONE or MULTIPLE recipes — return ALL of them in the recipes array
            - Recipes may be FULLY HANDWRITTEN — read all handwritten text carefully as recipe content
            - Only ignore handwritten text that is clearly a marginal annotation on a PRINTED recipe (checkmarks, "good!", crossed-out alternatives)
            - Two-column ingredient lists: read left column top-to-bottom, then right column top-to-bottom (do NOT read across rows)
            - Sub-recipe sections: look for "For the X:", "Dough:", "Filling:", "Sauce:", "Marinade:", "Frosting:", "Topping:", "Crust:", "Glaze:" — use the label as the section field to group ingredients
            - Valid units: TSP, TBSP, CUP, PINT, QUART, GALLON, HALF_GALLON, FL_OZ, WHOLE, LBS, OZ, PINCH, PIECE, G, ML, KG, L
            - Common abbreviations: c./C. = CUP, lb./lbs. = LBS, tsp./t. = TSP, tbsp./T. = TBSP, oz. = OZ, pkg. = PIECE, env. = PIECE, pt. = PINT, qt. = QUART
            - Use WHOLE for items counted by number (e.g., "3 eggs" -> quantity 3, unit WHOLE)
            - Use PINCH for "doonks", dashes, or pinches
            - NAME = shopping-list item. Strip ALL prep verbs (peeled, chopped, minced, sliced, diced, grated, beaten, etc.)
            - PREP NOTE = preparation guidance stripped from the name (e.g., "grated", "peeled and left whole", "beaten")
            - Convert unicode fractions to decimals (1/2=0.5, 1/4=0.25, 3/4=0.75, 1/3=0.333, 2/3=0.667)
            - For ranges like "3/4-1 cup" or "3 to 4 tablespoons", use the higher value
            - Number each instruction step
            - Do NOT include serving suggestions, family meal ideas, or non-recipe text in instructions
            - Ignore page numbers, book headers/footers, source attributions, and copyright notices
            - Return ONLY valid JSON, no markdown fences or extra text""";

    private static final String VISION_PROMPT = """
            You are a recipe extraction assistant. Analyze this photo of a recipe card or page and extract ALL recipes as JSON.

            Visual guidance:
            - The recipe may be HANDWRITTEN, PRINTED, or a mix — read ALL text as recipe content
            - Only ignore handwritten text that is clearly a marginal annotation on a printed recipe (checkmarks, "good!", crossed-out alternatives)
            - The image may be ROTATED 90° or upside down — orient yourself by finding the recipe title first
            - If ingredients are in two columns, read the LEFT column top-to-bottom first, then the RIGHT column
            - Look for sub-recipe section headers like "For the Dough:", "Filling:", "Sauce:" — group ingredients under these sections
            - Recipe title is usually in larger, bold, or underlined text at the top
            - For handwritten text, common abbreviations: c=cup, t/tsp=teaspoon, T/tbsp=tablespoon, lb=pound, oz=ounce, pkg=package
            - "Makes X" or "Serves X" indicates baseServings
            """ + RECIPE_SCHEMA + "\n\n" + RULES;

    private static final String TEXT_PROMPT_PREFIX = """
            You are a recipe extraction assistant. The following is raw OCR text from a scanned recipe photo. \
            The OCR may contain errors, merged lines, or text from multiple columns. Extract ALL recipes as JSON:
            """ + RECIPE_SCHEMA + "\n\n" + RULES + """

            Additional rules for OCR cleanup:
            - Separate ingredients from instructions even if they are merged in the text
            - Common OCR character errors: "l"/"I" misread as "1" or vice versa (e.g., "l cup"="1 cup"), "0"/"O" swapped (e.g., "35O"="350"), "rn" misread as "m", "cl" misread as "d"
            - Common OCR word errors: "cuo"="cup", "tep"="tsp", "tbep"="tbsp", "fiour"="flour", "saIt"="salt", "buiter"="butter", "suqar"="sugar"
            - Fraction misreads: "V2"="1/2", "VA"="1/4" — also handle unicode fractions
            - Two-column merge detection: if a line has two quantities (e.g., "2 cups flour 1 tsp salt"), it is likely two ingredients merged from side-by-side columns — split them
            - Look for sub-recipe sections: "For the Dough:", "Filling:", "Sauce:", "Marinade:" — use as section field
            - Ignore page numbers, book headers/footers, source attributions, and non-recipe text
            - If a recipe name seems truncated or garbled, infer it from context

            OCR Text:
            """;

    public OllamaRecipeExtractor(
            @Value("${ollama.host:}") String host,
            @Value("${ollama.port:11434}") int port,
            @Value("${ollama.vision-model:qwen2.5vl:7b}") String visionModel,
            @Value("${ollama.text-model:qwen2.5:32b-instruct-q4_K_M}") String textModel,
            @Value("${ollama.timeout:180}") int timeout,
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

    public List<ImportedRecipePreview> extractFromImage(byte[] imageBytes, String mimeType) {
        if (baseUrl.isBlank()) return List.of();
        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            // Native Ollama API uses "images" array with raw base64 (no data URI prefix)
            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", VISION_PROMPT,
                    "images", List.of(base64));
            Map<String, Object> body = Map.of(
                    "model", visionModel,
                    "messages", List.of(message),
                    "stream", false,
                    "think", false,
                    "options", Map.of("temperature", 0.1, "num_predict", 8192));

            String json = callOllama(body);
            return parseResponse(json);
        } catch (Exception e) {
            log.warn("Vision extraction failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<ImportedRecipePreview> extractFromText(String ocrText) {
        if (baseUrl.isBlank()) return List.of();
        try {
            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", TEXT_PROMPT_PREFIX + ocrText);
            Map<String, Object> body = Map.of(
                    "model", textModel,
                    "messages", List.of(message),
                    "stream", false,
                    "think", false,
                    "options", Map.of("temperature", 0.1, "num_predict", 8192));

            String json = callOllama(body);
            return parseResponse(json);
        } catch (Exception e) {
            log.warn("Text extraction failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String callOllama(Map<String, Object> body) throws Exception {
        String requestBody = objectMapper.writeValueAsString(body);
        log.debug("Calling Ollama native API: {}", baseUrl + "/api/chat");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(timeout))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode message = root.path("message");

        // Primary: read from content field
        String text = message.path("content").asText("").strip();
        if (!text.isEmpty()) return text;

        // Fallback: Qwen3 thinking models put chain-of-thought in "thinking" field
        // and may leave content empty — try to extract JSON from thinking
        String thinking = message.path("thinking").asText("").strip();
        if (!thinking.isEmpty()) {
            log.warn("Content field empty, attempting to extract JSON from thinking field ({} chars)", thinking.length());
            return extractJsonFromThinking(thinking);
        }

        throw new RuntimeException("Ollama response had empty content and no thinking field");
    }

    private String extractJsonFromThinking(String text) {
        // Find the last top-level { ... } block (likely the final JSON answer)
        int depth = 0;
        int lastObjStart = -1;
        int lastObjEnd = -1;
        boolean inString = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    if (depth == 0) lastObjStart = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && lastObjStart >= 0) {
                        lastObjEnd = i + 1;
                    }
                }
            }
        }

        if (lastObjStart >= 0 && lastObjEnd > lastObjStart) {
            return text.substring(lastObjStart, lastObjEnd);
        }

        throw new RuntimeException("No JSON object found in thinking text");
    }

    List<ImportedRecipePreview> parseResponse(String content) {
        String json = content.strip();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        List<ImportedRecipePreview> results = new ArrayList<>();

        // Try multi-recipe format first
        try {
            LlmMultiRecipeResponse multi = objectMapper.readValue(json, LlmMultiRecipeResponse.class);
            if (multi.recipes != null && !multi.recipes.isEmpty()) {
                for (LlmRecipeResponse recipe : multi.recipes) {
                    ImportedRecipePreview preview = buildPreview(recipe);
                    if (preview != null) results.add(preview);
                }
                return results;
            }
        } catch (Exception ignored) {
            // Fall through to single-recipe format
        }

        // Fall back to single-recipe format (backward compatibility)
        try {
            LlmRecipeResponse single = objectMapper.readValue(json, LlmRecipeResponse.class);
            ImportedRecipePreview preview = buildPreview(single);
            if (preview != null) results.add(preview);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to parse LLM response as recipe JSON", e);
        }

        return results;
    }

    private ImportedRecipePreview buildPreview(LlmRecipeResponse llm) {
        if (llm == null || llm.name == null || llm.name.isBlank()) return null;

        List<ImportedIngredientPreview> ingredients = new ArrayList<>();
        if (llm.ingredients != null) {
            for (LlmIngredient ing : llm.ingredients) {
                String unit = ing.unit != null ? ing.unit.toUpperCase() : "WHOLE";
                String name = ing.name != null ? ing.name.trim() : "";
                if (name.isBlank()) continue;

                BigDecimal quantity = ing.quantity != null ? ing.quantity : BigDecimal.ONE;
                String rawText = quantity + " " + unit + " " + name;

                ingredients.add(new ImportedIngredientPreview(
                        ing.section,
                        name,
                        quantity,
                        unit,
                        rawText,
                        ing.prepNote));
            }
        }

        return new ImportedRecipePreview(
                llm.name,
                llm.instructions != null ? llm.instructions : "",
                llm.baseServings > 0 ? llm.baseServings : 1,
                ingredients,
                "scan");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmMultiRecipeResponse(
            List<LlmRecipeResponse> recipes
    ) {}

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
            String unit,
            String prepNote
    ) {}
}
