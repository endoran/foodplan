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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
            - Sub-recipe sections: look for "For the X:", "Dough:", "Filling:", "Sauce:", "Marinade:", "Frosting:", "Topping:", "Crust:", "Glaze:" — use the label as the section field to group ingredients. \
            Ingredient names are NEVER sections. A section groups multiple ingredients under a sub-recipe label. If no clear section headers exist, set section to null.
            - Valid units (MUST be quoted strings in JSON): "TSP", "TBSP", "CUP", "PINT", "QUART", "GALLON", "HALF_GALLON", "FL_OZ", "WHOLE", "LBS", "OZ", "PINCH", "PIECE", "G", "ML", "KG", "L"
            - Common abbreviations: c./C. = CUP, lb./lbs. = LBS, tsp./t. = TSP, tbsp./T. = TBSP, oz. = OZ, pkg. = PIECE, env. = PIECE, pt. = PINT, qt. = QUART
            - CRITICAL unit abbreviation rules for HANDWRITTEN recipes: \
            A lowercase "t" or "t." before or after a number ALWAYS means TSP. \
            An uppercase "T" or "T." before or after a number ALWAYS means TBSP. \
            A lowercase "c" or "c." ALWAYS means CUP. \
            Examples: "1 t salt" = 1 TSP salt, "2 T butter" = 2 TBSP butter, "1c milk" = 1 CUP milk, "1/2 c flour" = 0.5 CUP flour. \
            NEVER map these to WHOLE — they are always measurement abbreviations.
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

    private static final Set<String> VALID_UNITS = Set.of(
            "TSP", "TBSP", "FL_OZ", "CUP", "PINT", "QUART", "GALLON", "HALF_GALLON",
            "WHOLE", "LBS", "OZ", "PINCH", "PIECE", "G", "ML", "KG", "L");

    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
            Map.entry("TEASPOON", "TSP"), Map.entry("TEASPOONS", "TSP"),
            Map.entry("TABLESPOON", "TBSP"), Map.entry("TABLESPOONS", "TBSP"),
            Map.entry("POUND", "LBS"), Map.entry("POUNDS", "LBS"), Map.entry("LB", "LBS"),
            Map.entry("OUNCE", "OZ"), Map.entry("OUNCES", "OZ"),
            Map.entry("FLUID_OUNCE", "FL_OZ"), Map.entry("FLUID_OUNCES", "FL_OZ"),
            Map.entry("CLOVE", "WHOLE"), Map.entry("CLOVES", "WHOLE"),
            Map.entry("HEAD", "WHOLE"), Map.entry("HEADS", "WHOLE"),
            Map.entry("SLICE", "PIECE"), Map.entry("SLICES", "PIECE"),
            Map.entry("PACKAGE", "PIECE"), Map.entry("PACKAGES", "PIECE"),
            Map.entry("PKG", "PIECE"), Map.entry("ENVELOPE", "PIECE"),
            Map.entry("CAN", "PIECE"), Map.entry("CANS", "PIECE"),
            Map.entry("BOTTLE", "PIECE"), Map.entry("BOTTLES", "PIECE"),
            Map.entry("BUNCH", "PIECE"), Map.entry("BUNCHES", "PIECE"),
            Map.entry("DASH", "PINCH"), Map.entry("DASHES", "PINCH"),
            Map.entry("DOONK", "PINCH"), Map.entry("DOONKS", "PINCH"),
            Map.entry("GRAM", "G"), Map.entry("GRAMS", "G"),
            Map.entry("MILLILITER", "ML"), Map.entry("MILLILITERS", "ML"),
            Map.entry("KILOGRAM", "KG"), Map.entry("KILOGRAMS", "KG"),
            Map.entry("LITER", "L"), Map.entry("LITERS", "L"),
            Map.entry("GALLON_HALF", "HALF_GALLON"),
            Map.entry("CUPS", "CUP"), Map.entry("PINTS", "PINT"),
            Map.entry("QUARTS", "QUART"), Map.entry("GALLONS", "GALLON"));

    // Unit abbreviation rules are intentionally duplicated between VISION_PROMPT and RULES:
    // vision models need critical rules reinforced in the immediate prompt context, not just in
    // a distant shared rules section, to reliably apply them.
    private static final String VISION_PROMPT = """
            You are a recipe extraction assistant. Analyze this photo of a recipe card or page and extract ALL recipes as JSON.

            Visual guidance:
            - The recipe may be HANDWRITTEN, PRINTED, or a mix — read ALL text as recipe content
            - Only ignore handwritten text that is clearly a marginal annotation on a printed recipe (checkmarks, "good!", crossed-out alternatives)
            - The image may be ROTATED 90° or upside down — orient yourself by finding the recipe title first
            - RECIPE TITLE: Look for the most prominent text (larger, bold, decorative font). \
            Ignore small diet/lifestyle tags like "NSI", "DF", "S", "E", "FP", "FRIENDLY", "KETO", "PALEO" — these are NOT the recipe name.
            - If ingredients are in two columns, read the LEFT column top-to-bottom first, then the RIGHT column
            - Look for sub-recipe section headers like "For the Dough:", "Filling:", "Sauce:" — group ingredients under these sections. \
            IMPORTANT: Individual ingredient names (butter, milk, eggs, garlic, etc.) are NEVER section names. \
            A section is a sub-recipe label that groups multiple ingredients (e.g., "Marinade", "Sauce", "Dough"). \
            If there are no clear multi-ingredient section headers, set section to null for ALL ingredients.
            - For handwritten text, UNIT ABBREVIATION is CRITICAL: \
            lowercase "t" or "t." = TSP (teaspoon), uppercase "T" or "T." = TBSP (tablespoon), \
            "c" or "c." = CUP, "lb" = LBS, "oz" = OZ, "pkg" = PIECE. \
            Example: "1 t salt" = {"quantity": 1, "unit": "TSP"}, "2 T butter" = {"quantity": 2, "unit": "TBSP"}, "1c milk" = {"quantity": 1, "unit": "CUP"}. \
            NEVER use WHOLE when a measurement abbreviation is present.
            - If a recipe card has multiple columns for different serving sizes (single/double/triple/quad), \
            extract ONLY the first (smallest) column values
            - "Makes X" or "Serves X" indicates baseServings
            """ + RECIPE_SCHEMA + "\n\n" + RULES;

    private static final String TEXT_PROMPT_PREFIX = """
            You are a recipe extraction assistant. The following is raw OCR text from a scanned recipe photo. \
            The OCR may contain errors, merged lines, or text from multiple columns. Extract ALL recipes as JSON:
            """ + RECIPE_SCHEMA + "\n\n" + RULES + """

            Additional rules for OCR cleanup:
            - RECIPE TITLE (CRITICAL): The recipe name is the most important field to get right. \
            It is usually a descriptive phrase like "Oatmeal on the Go Cups" or "Chicken Caesar Salad". \
            NEVER use a diet/lifestyle tag as the recipe name. These are TAGS, not recipe names: \
            "FRIENDLY", "NSI", "DF", "S", "E", "FP", "KETO", "PALEO", "GF", "DAIRY FREE", "GLUTEN FREE", \
            "THM", "TRIM HEALTHY", "WHOLE30", "LOW CARB", "SUGAR FREE", "VEGAN", "VEGETARIAN". \
            If the OCR title line is garbled (e.g., "REE ERE HEE EEE"), skip it and derive the name \
            from the recipe content: key ingredients + cooking method (e.g., oats + muffin tin = "Oatmeal Muffin Cups"). \
            Also ignore book titles (e.g., "trim healthy table"), chapter headers, and page numbers.
            - Separate ingredients from instructions even if they are merged in the text
            - Common OCR character errors: "l"/"I" misread as "1" or vice versa (e.g., "l cup"="1 cup"), "0"/"O" swapped (e.g., "35O"="350"), "rn" misread as "m", "cl" misread as "d"
            - Common OCR word errors: "cuo"="cup", "tep"="tsp", "tbep"="tbsp", "fiour"="flour", "saIt"="salt", "buiter"="butter", "suqar"="sugar"
            - Fraction misreads: "V2"="1/2", "VA"="1/4" — also handle unicode fractions
            - Two-column merge detection: if a line has two quantities (e.g., "2 cups flour 1 tsp salt"), it is likely two ingredients merged from side-by-side columns — split them
            - Look for sub-recipe sections: "For the Dough:", "Filling:", "Sauce:", "Marinade:" — use as section field. \
            IMPORTANT: Ingredient names are NEVER section names. A section is a sub-recipe grouping that multiple \
            ingredients belong to. If there are no clear section headers, set section to null for all ingredients.
            - Ignore page numbers, book headers/footers, source attributions, and non-recipe text (footnotes like "FOR NSI: USE A GROCERY...")
            - If a recipe name seems truncated or garbled, infer it from context clues (ingredients, cooking method)

            OCR Text:
            """;

    public OllamaRecipeExtractor(
            @Value("${ollama.host:}") String host,
            @Value("${ollama.port:11434}") int port,
            @Value("${ollama.vision-model:qwen2.5vl:32b}") String visionModel,
            @Value("${ollama.text-model:qwen2.5:32b-instruct-q4_K_M}") String textModel,
            @Value("${ollama.timeout:300}") int timeout,
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
            String response = callVisionModel(imageBytes);
            return parseResponse(response);
        } catch (Exception e) {
            log.warn("Vision extraction failed: {}", e.getMessage());
            return List.of();
        }
    }

    public Optional<String> callVisionModelRaw(byte[] imageBytes) {
        if (baseUrl.isBlank()) return Optional.empty();
        try {
            return Optional.of(callVisionModel(imageBytes));
        } catch (Exception e) {
            log.warn("Vision model call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String callVisionModel(byte[] imageBytes) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", VISION_PROMPT,
                "images", List.of(base64));
        Map<String, Object> body = Map.of(
                "model", visionModel,
                "messages", List.of(message),
                "stream", false,
                "think", false,
                "options", Map.of("temperature", 0.1, "num_predict", 8192, "num_ctx", 16384));

        return callOllama(body);
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
                    "options", Map.of("temperature", 0.1, "num_predict", 8192, "num_ctx", 16384));

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

    public List<ImportedRecipePreview> parseResponse(String content) {
        String json = content.strip();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```\\s*$", "");
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
                log.info("Parsed {} recipe(s) via multi-recipe format", results.size());
                return results;
            }
        } catch (Exception e) {
            log.debug("Multi-recipe parse failed (trying single): {}", e.getMessage());
        }

        // Fall back to single-recipe format (backward compatibility)
        try {
            LlmRecipeResponse single = objectMapper.readValue(json, LlmRecipeResponse.class);
            ImportedRecipePreview preview = buildPreview(single);
            if (preview != null) results.add(preview);
            log.info("Parsed {} recipe(s) via single-recipe format", results.size());
            return results;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.debug("Single-recipe JSON parse failed: {}", e.getMessage());
        }

        // Repair attempt: fix unquoted enum-style values (e.g., "unit": CUP → "unit": "CUP")
        String repaired = json.replaceAll("\":\\s+([A-Z][A-Z_]{0,19})\\s*([,\\}\\]])", "\": \"$1\"$2");
        if (!repaired.equals(json)) {
            log.debug("Retrying JSON parse after quoting bare enum values");
            try {
                LlmMultiRecipeResponse multi = objectMapper.readValue(repaired, LlmMultiRecipeResponse.class);
                if (multi.recipes != null && !multi.recipes.isEmpty()) {
                    for (LlmRecipeResponse recipe : multi.recipes) {
                        ImportedRecipePreview preview = buildPreview(recipe);
                        if (preview != null) results.add(preview);
                    }
                    log.info("Parsed {} recipe(s) after JSON enum repair", results.size());
                    return results;
                }
            } catch (Exception e) {
                log.debug("Repaired multi-recipe parse also failed: {}", e.getMessage());
            }
        }

        throw new RuntimeException("Failed to parse LLM response as recipe JSON");
    }

    private static final Set<String> DIET_TAGS = Set.of(
            "FRIENDLY", "NSI", "DF", "FP", "KETO", "PALEO", "VEGAN", "VEGETARIAN",
            "GF", "GLUTEN FREE", "DAIRY FREE", "LOW CARB", "SUGAR FREE", "WHOLE30",
            "S", "E", "THM", "TRIM HEALTHY", "TRIM HEALTHY MAMA");

    private ImportedRecipePreview buildPreview(LlmRecipeResponse llm) {
        if (llm == null || llm.name == null || llm.name.isBlank()) return null;

        String recipeName = llm.name.trim();
        if (DIET_TAGS.contains(recipeName.toUpperCase())) {
            log.warn("LLM returned diet tag '{}' as recipe name — deriving from ingredients", recipeName);
            recipeName = deriveNameFromIngredients(llm.ingredients);
        }

        List<ImportedIngredientPreview> ingredients = new ArrayList<>();
        if (llm.ingredients != null) {
            for (LlmIngredient ing : llm.ingredients) {
                String unit = normalizeUnit(ing.unit);
                String name = ing.name != null ? ing.name.trim() : "";
                if (name.isBlank()) continue;

                BigDecimal quantity = parseQuantity(ing.quantity);
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
                recipeName,
                instructionsToString(llm.instructions),
                llm.baseServings > 0 ? llm.baseServings : 1,
                ingredients,
                "scan");
    }

    private static String instructionsToString(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            int step = 1;
            for (JsonNode item : node) {
                String text = item.isTextual() ? item.asText() : item.toString();
                if (!text.matches("^\\d+\\.\\s.*")) {
                    sb.append(step).append(". ");
                }
                sb.append(text).append("\n");
                step++;
            }
            return sb.toString().strip();
        }
        return node.toString();
    }

    private static BigDecimal parseQuantity(JsonNode node) {
        if (node == null || node.isNull()) return BigDecimal.ONE;
        if (node.isNumber()) return node.decimalValue();
        if (node.isTextual()) {
            String text = node.asText().strip();
            if (text.isEmpty()) return BigDecimal.ONE;
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException e) {
                return parseFraction(text);
            }
        }
        return BigDecimal.ONE;
    }

    private static BigDecimal parseFraction(String text) {
        // Handle mixed fractions like "1 1/2"
        String[] parts = text.split("\\s+");
        BigDecimal whole = BigDecimal.ZERO;
        String fractionPart = text;
        if (parts.length == 2 && parts[1].contains("/")) {
            try {
                whole = new BigDecimal(parts[0]);
                fractionPart = parts[1];
            } catch (NumberFormatException e) {
                return BigDecimal.ONE;
            }
        }
        if (fractionPart.contains("/")) {
            String[] frac = fractionPart.split("/");
            if (frac.length == 2) {
                try {
                    BigDecimal num = new BigDecimal(frac[0].strip());
                    BigDecimal den = new BigDecimal(frac[1].strip());
                    if (den.compareTo(BigDecimal.ZERO) != 0) {
                        return whole.add(num.divide(den, 4, RoundingMode.HALF_UP));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return BigDecimal.ONE;
    }

    private static String deriveNameFromIngredients(List<LlmIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) return "Untitled Recipe";
        List<String> keyIngredients = new ArrayList<>();
        for (LlmIngredient ing : ingredients) {
            if (ing.name == null) continue;
            String n = ing.name.trim().toLowerCase();
            if (n.contains("salt") || n.contains("pepper") || n.contains("oil") || n.contains("spray")
                    || n.contains("water") || n.contains("sugar") || n.contains("flour")
                    || n.contains("butter") || n.contains("extract") || n.contains("baking")) continue;
            if (keyIngredients.size() < 2) {
                String capitalized = n.substring(0, 1).toUpperCase() + n.substring(1);
                keyIngredients.add(capitalized);
            }
        }
        if (keyIngredients.isEmpty()) return "Untitled Recipe";
        return String.join(" & ", keyIngredients) + " Recipe";
    }

    private static String normalizeUnit(String raw) {
        if (raw == null || raw.isBlank()) return "WHOLE";
        String upper = raw.strip().toUpperCase().replace(" ", "_");
        if (VALID_UNITS.contains(upper)) return upper;
        String mapped = UNIT_ALIASES.get(upper);
        if (mapped != null) return mapped;
        log.debug("Unmapped unit '{}', defaulting to WHOLE", raw);
        return "WHOLE";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmMultiRecipeResponse(
            List<LlmRecipeResponse> recipes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmRecipeResponse(
            String name,
            int baseServings,
            JsonNode instructions,
            List<LlmIngredient> ingredients
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LlmIngredient(
            String section,
            String name,
            JsonNode quantity,
            String unit,
            String prepNote
    ) {}
}
