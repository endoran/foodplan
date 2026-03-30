package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecipeImportService {

    private static final Pattern QTY_PATTERN = Pattern.compile(
            "^\\s*(\\d+\\s+\\d+/\\d+|\\d+(?:[./]\\d+)?)\\s*");

    // Unicode fraction characters OCR may produce
    private static final Map<Character, String> UNICODE_FRACTIONS = Map.ofEntries(
            Map.entry('\u00BC', "1/4"), Map.entry('\u00BD', "1/2"), Map.entry('\u00BE', "3/4"),
            Map.entry('\u2153', "1/3"), Map.entry('\u2154', "2/3"),
            Map.entry('\u2155', "1/5"), Map.entry('\u2156', "2/5"), Map.entry('\u2157', "3/5"),
            Map.entry('\u2158', "4/5"), Map.entry('\u2159', "1/6"), Map.entry('\u215A', "5/6"),
            Map.entry('\u215B', "1/8"), Map.entry('\u215C', "3/8"),
            Map.entry('\u215D', "5/8"), Map.entry('\u215E', "7/8")
    );

    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
            // Teaspoon
            Map.entry("teaspoon", "TSP"), Map.entry("teaspoons", "TSP"), Map.entry("tsp", "TSP"),
            Map.entry("tep", "TSP"), Map.entry("t5p", "TSP"), Map.entry("tsp.", "TSP"),
            // Tablespoon
            Map.entry("tablespoon", "TBSP"), Map.entry("tablespoons", "TBSP"), Map.entry("tbsp", "TBSP"),
            Map.entry("tosp", "TBSP"), Map.entry("thsp", "TBSP"), Map.entry("tbsp.", "TBSP"),
            Map.entry("tbs", "TBSP"), Map.entry("tabiespoon", "TBSP"), Map.entry("tabiespoons", "TBSP"),
            Map.entry("tablespcon", "TBSP"), Map.entry("tablespo0n", "TBSP"),
            // Cup
            Map.entry("cup", "CUP"), Map.entry("cups", "CUP"), Map.entry("c", "CUP"),
            Map.entry("cuo", "CUP"), Map.entry("cus", "CUP"), Map.entry("cuos", "CUP"),
            // Pint
            Map.entry("pint", "PINT"), Map.entry("pints", "PINT"),
            // Quart
            Map.entry("quart", "QUART"), Map.entry("quarts", "QUART"),
            // Gallon
            Map.entry("gallon", "GALLON"), Map.entry("gallons", "GALLON"),
            // Ounce
            Map.entry("ounce", "OZ"), Map.entry("ounces", "OZ"), Map.entry("oz", "OZ"),
            Map.entry("0z", "OZ"), Map.entry("oz.", "OZ"),
            // Pound
            Map.entry("pound", "LBS"), Map.entry("pounds", "LBS"), Map.entry("lb", "LBS"),
            Map.entry("lbs", "LBS"), Map.entry("1b", "LBS"), Map.entry("1bs", "LBS"),
            Map.entry("ibs", "LBS"), Map.entry("ib", "LBS"),
            // Pinch
            Map.entry("pinch", "PINCH"),
            // Piece
            Map.entry("piece", "PIECE"), Map.entry("pieces", "PIECE"),
            // Descriptive units
            Map.entry("whole", "UNIT"), Map.entry("large", "UNIT"), Map.entry("medium", "UNIT"),
            Map.entry("small", "UNIT"), Map.entry("clove", "UNIT"), Map.entry("cloves", "UNIT")
    );

    private static final Pattern UNIT_PATTERN;

    static {
        String unitAlternation = String.join("|", UNIT_ALIASES.keySet().stream()
                .sorted((a, b) -> b.length() - a.length()).toList());
        UNIT_PATTERN = Pattern.compile(
                "^\\s*(?:\\d+\\s+\\d+/\\d+|\\d+(?:[./]\\d+)?)\\s*(" + unitAlternation + ")\\.?\\s+",
                Pattern.CASE_INSENSITIVE);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImportedRecipePreview importFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("FoodPlan/1.0")
                .timeout(10_000)
                .get();

        // Try JSON-LD first
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            try {
                JsonNode node = objectMapper.readTree(script.data());
                ImportedRecipePreview result = parseJsonLd(node, url);
                if (result != null) return result;
            } catch (Exception ignored) {}
        }

        throw new RecipeImportException("No structured recipe data (JSON-LD) found at this URL");
    }

    private ImportedRecipePreview parseJsonLd(JsonNode node, String url) {
        // Handle @graph arrays
        if (node.has("@graph")) {
            for (JsonNode item : node.get("@graph")) {
                ImportedRecipePreview result = parseRecipeNode(item, url);
                if (result != null) return result;
            }
            return null;
        }

        // Handle arrays
        if (node.isArray()) {
            for (JsonNode item : node) {
                ImportedRecipePreview result = parseJsonLd(item, url);
                if (result != null) return result;
            }
            return null;
        }

        return parseRecipeNode(node, url);
    }

    private ImportedRecipePreview parseRecipeNode(JsonNode node, String url) {
        String type = node.path("@type").asText("");
        if (!type.equalsIgnoreCase("Recipe")) return null;

        String name = node.path("name").asText("Untitled Recipe");

        // Instructions
        String instructions = "";
        JsonNode instructionNode = node.path("recipeInstructions");
        if (instructionNode.isTextual()) {
            instructions = instructionNode.asText();
        } else if (instructionNode.isArray()) {
            List<String> steps = new ArrayList<>();
            int stepNum = 1;
            for (JsonNode step : instructionNode) {
                if (step.isTextual()) {
                    steps.add(stepNum++ + ". " + step.asText());
                } else if (step.has("text")) {
                    steps.add(stepNum++ + ". " + step.path("text").asText());
                }
            }
            instructions = String.join("\n", steps);
        }

        // Servings
        int servings = 1;
        JsonNode yieldNode = node.path("recipeYield");
        if (yieldNode.isTextual()) {
            Matcher m = Pattern.compile("(\\d+)").matcher(yieldNode.asText());
            if (m.find()) servings = Integer.parseInt(m.group(1));
        } else if (yieldNode.isArray() && !yieldNode.isEmpty()) {
            Matcher m = Pattern.compile("(\\d+)").matcher(yieldNode.get(0).asText());
            if (m.find()) servings = Integer.parseInt(m.group(1));
        } else if (yieldNode.isNumber()) {
            servings = yieldNode.asInt(1);
        }

        // Ingredients
        List<ImportedIngredientPreview> ingredients = new ArrayList<>();
        JsonNode ingredientNode = node.path("recipeIngredient");
        if (ingredientNode.isArray()) {
            for (JsonNode ing : ingredientNode) {
                String raw = ing.asText("").trim();
                if (!raw.isEmpty()) {
                    ingredients.add(parseIngredientText(raw));
                }
            }
        }

        return new ImportedRecipePreview(name, instructions, servings, ingredients, url);
    }

    ImportedIngredientPreview parseIngredientText(String raw) {
        String normalized = normalizeUnicodeFractions(raw);
        BigDecimal quantity = BigDecimal.ONE;
        String unit = "UNIT";
        String ingredientName = normalized;

        // Extract quantity
        Matcher qtyMatcher = QTY_PATTERN.matcher(normalized);
        if (qtyMatcher.find()) {
            String qtyStr = qtyMatcher.group(1);
            quantity = parseFraction(qtyStr);
            ingredientName = normalized.substring(qtyMatcher.end()).trim();
        }

        // Extract unit (exact match via regex)
        Matcher unitMatcher = UNIT_PATTERN.matcher(normalized);
        if (unitMatcher.find()) {
            String matchedUnit = unitMatcher.group(1).toLowerCase();
            unit = UNIT_ALIASES.getOrDefault(matchedUnit, "UNIT");
            ingredientName = normalized.substring(unitMatcher.end()).trim();
        } else if (qtyMatcher.hitEnd() || qtyMatcher.find(0)) {
            // Fuzzy fallback: check if the first word after quantity is close to a known unit
            String afterQty = ingredientName;
            String[] words = afterQty.split("\\s+", 2);
            if (words.length >= 1 && !words[0].isEmpty()) {
                String candidate = words[0].toLowerCase().replaceAll("\\.$", "");
                String fuzzyMatch = fuzzyMatchUnit(candidate);
                if (fuzzyMatch != null) {
                    unit = fuzzyMatch;
                    ingredientName = words.length > 1 ? words[1].trim() : "";
                }
            }
        }

        // Clean up ingredient name
        ingredientName = ingredientName.replaceAll("^of\\s+", "")
                .replaceAll(",.*", "")
                .trim();

        if (ingredientName.isEmpty()) {
            ingredientName = raw;
        }

        return new ImportedIngredientPreview(ingredientName, quantity, unit, raw);
    }

    private BigDecimal parseFraction(String s) {
        // Mixed number: "1 1/2" → 1.5
        if (s.contains(" ") && s.contains("/")) {
            String[] parts = s.trim().split("\\s+");
            if (parts.length == 2) {
                try {
                    BigDecimal whole = new BigDecimal(parts[0]);
                    BigDecimal frac = parseFraction(parts[1]);
                    return whole.add(frac);
                } catch (Exception e) {
                    return BigDecimal.ONE;
                }
            }
        }
        if (s.contains("/")) {
            String[] parts = s.split("/");
            if (parts.length == 2) {
                try {
                    return new BigDecimal(parts[0]).divide(new BigDecimal(parts[1]), 4, java.math.RoundingMode.HALF_UP);
                } catch (Exception e) {
                    return BigDecimal.ONE;
                }
            }
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    private String fuzzyMatchUnit(String candidate) {
        if (candidate.length() < 2 || candidate.length() > 12) return null;
        // Don't fuzzy-match words that look like ingredient names (all alpha, > 5 chars, no resemblance)
        int bestDist = Integer.MAX_VALUE;
        String bestUnit = null;
        for (Map.Entry<String, String> entry : UNIT_ALIASES.entrySet()) {
            String alias = entry.getKey();
            // Only fuzzy-match against aliases of similar length
            if (Math.abs(alias.length() - candidate.length()) > 1) continue;
            int dist = editDistance(candidate, alias);
            if (dist <= 1 && dist < bestDist) {
                bestDist = dist;
                bestUnit = entry.getValue();
                if (dist == 0) break;
            }
        }
        return bestUnit;
    }

    private static int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private String normalizeUnicodeFractions(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String replacement = UNICODE_FRACTIONS.get(c);
            if (replacement != null) {
                // If preceded by a digit (e.g., "1½"), insert a space so it becomes "1 1/2"
                if (sb.length() > 0 && Character.isDigit(sb.charAt(sb.length() - 1))) {
                    sb.append(' ');
                }
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
