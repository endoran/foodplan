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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecipeImportService {

    private static final Logger log = LoggerFactory.getLogger(RecipeImportService.class);

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
            Map.entry("tosp", "TBSP"), Map.entry("thsp", "TBSP"), Map.entry("tposp", "TBSP"),
            Map.entry("tbsp.", "TBSP"), Map.entry("tbs", "TBSP"),
            Map.entry("tabiespoon", "TBSP"), Map.entry("tabiespoons", "TBSP"),
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
            // Can → WHOLE
            Map.entry("can", "WHOLE"), Map.entry("cans", "WHOLE"),
            // Pinch
            Map.entry("pinch", "PINCH"),
            // Piece
            Map.entry("piece", "PIECE"), Map.entry("pieces", "PIECE"),
            // Metric
            Map.entry("g", "G"), Map.entry("gram", "G"), Map.entry("grams", "G"),
            Map.entry("ml", "ML"), Map.entry("milliliter", "ML"), Map.entry("milliliters", "ML"),
            Map.entry("millilitre", "ML"), Map.entry("millilitres", "ML"),
            Map.entry("kg", "KG"), Map.entry("kilogram", "KG"), Map.entry("kilograms", "KG"),
            Map.entry("l", "L"), Map.entry("liter", "L"), Map.entry("liters", "L"),
            Map.entry("litre", "L"), Map.entry("litres", "L"),
            // Descriptive units → WHOLE
            Map.entry("whole", "WHOLE"), Map.entry("large", "WHOLE"), Map.entry("medium", "WHOLE"),
            Map.entry("small", "WHOLE"), Map.entry("clove", "WHOLE"), Map.entry("cloves", "WHOLE"),
            Map.entry("bulb", "WHOLE"), Map.entry("bunch", "WHOLE"), Map.entry("head", "WHOLE"),
            Map.entry("stalk", "WHOLE"), Map.entry("stalks", "WHOLE"),
            Map.entry("sprig", "WHOLE"), Map.entry("sprigs", "WHOLE"),
            Map.entry("fresh", "WHOLE")
    );

    private static final Pattern UNIT_PATTERN;

    static {
        String unitAlternation = String.join("|", UNIT_ALIASES.keySet().stream()
                .sorted((a, b) -> b.length() - a.length()).toList());
        UNIT_PATTERN = Pattern.compile(
                "^\\s*(?:\\d+\\s+\\d+/\\d+|\\d+(?:[./]\\d+)?)\\s*(" + unitAlternation + ")\\.?\\s+",
                Pattern.CASE_INSENSITIVE);
    }

    // Number words for descriptive prefixes like "One 3-pound chuck roast"
    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
            Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3),
            Map.entry("four", 4), Map.entry("five", 5), Map.entry("six", 6),
            Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9),
            Map.entry("ten", 10), Map.entry("eleven", 11), Map.entry("twelve", 12),
            Map.entry("a", 1), Map.entry("an", 1)
    );

    // Pre-processing patterns
    private static final Pattern DASH_FRACTION = Pattern.compile("(\\d+)-(\\d+/\\d+)");
    private static final Pattern RANGE_WITH_UNIT = Pattern.compile(
            "(\\d+(?:\\s+\\d+/\\d+|[./]\\d+)?)-?\\s*to\\s*(\\d+(?:\\s+\\d+/\\d+|[./]\\d+)?)\\s*[- ]?(\\w+)");
    private static final Pattern RANGE_SIMPLE = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s+to\\s+(\\d+(?:\\.\\d+)?)(?=\\s)");
    private static final Pattern PAREN_MEASUREMENT = Pattern.compile(
            "\\((\\d+(?:[- ]\\d+/\\d+|[./]\\d+)?)\\s*-?\\s*(ounces?|oz|pounds?|lbs?|grams?|g)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_HEADER = Pattern.compile("^([A-Z][A-Z ]+):$");
    private static final Pattern NUMBER_WORD_PREFIX = Pattern.compile(
            "^(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|a|an)\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DUAL_UNIT = Pattern.compile(
            "(\\d+(?:\\s+\\d+/\\d+|[./]\\d+)?)\\s*(cups?|tablespoons?|teaspoons?)/(\\d+)\\s*(grams?|g|ml|oz|ounces?)\\s+",
            Pattern.CASE_INSENSITIVE);

    // Post-processing patterns
    private static final Pattern PLUS_MINUS_MOD = Pattern.compile(
            "\\s*(?:plus|minus)\\s+(?:\\d+(?:\\s+\\d+/\\d+)?\\s+(?:tablespoons?|teaspoons?|cups?|tbsp\\.?|tsp\\.?)\\s*(?:for\\s+\\w+(?:\\s+\\w+)*)?|(?:a\\s+)?(?:little|more|extra)\\b.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PAREN_ALT_MEASURE = Pattern.compile(
            "\\((?:\\d+[^)]*(?:ounces?|sticks?|grams?|g|oz|lbs?|pounds?|cups?|ml)|such as[^)]*|or [^)]*)\\)?",
            Pattern.CASE_INSENSITIVE);
    // Strips "Brand1 or qty unit Brand2" leaving just the ingredient type at the end
    // e.g. "Diamond Crystal or 1/2 tsp. Morton kosher salt" → "kosher salt"
    private static final Pattern BRAND_ALTERNATIVE = Pattern.compile(
            "(?:Diamond Crystal|Morton)\\s+or\\s+(?:\\d+(?:\\s+\\d+/\\d+|/\\d+)?\\s*(?:tsp\\.?|tbsp\\.?|teaspoons?|tablespoons?)\\s*\\.?\\s*)?(?:Diamond Crystal|Morton)\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PACKAGE_DESC = Pattern.compile(
            "^package\\s+", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OllamaIngredientParser ollamaIngredientParser;

    public RecipeImportService(OllamaIngredientParser ollamaIngredientParser) {
        this.ollamaIngredientParser = ollamaIngredientParser;
    }

    public ImportedRecipePreview importFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
        String instructions = extractInstructions(node);
        int servings = extractServings(node);

        // Extract raw ingredient strings
        List<String> rawIngredientStrings = new ArrayList<>();
        JsonNode ingredientNode = node.path("recipeIngredient");
        if (ingredientNode.isArray()) {
            for (JsonNode ing : ingredientNode) {
                String raw = ing.asText("").trim();
                if (!raw.isEmpty()) rawIngredientStrings.add(raw);
            }
        }

        // Try LLM-powered parsing first
        if (ollamaIngredientParser.isAvailable()) {
            try {
                OllamaIngredientParser.ParsedIngredientResult llmResult =
                        ollamaIngredientParser.parseIngredients(rawIngredientStrings, instructions);
                if (llmResult != null && !llmResult.ingredients().isEmpty()) {
                    log.info("LLM parsed {} ingredients from {} raw inputs", llmResult.ingredients().size(), rawIngredientStrings.size());
                    String enrichedInstructions = llmResult.enrichedInstructions() != null
                            ? llmResult.enrichedInstructions() : instructions;
                    return new ImportedRecipePreview(name, enrichedInstructions, servings,
                            llmResult.ingredients(), url);
                }
            } catch (Exception e) {
                log.warn("LLM ingredient parsing failed, using regex fallback: {}", e.getMessage());
            }
        }

        // Regex fallback
        List<ImportedIngredientPreview> ingredients = parseIngredientsWithRegex(rawIngredientStrings);
        return new ImportedRecipePreview(name, instructions, servings, ingredients, url);
    }

    private String extractInstructions(JsonNode node) {
        JsonNode instructionNode = node.path("recipeInstructions");
        if (instructionNode.isTextual()) {
            return instructionNode.asText();
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
            return String.join("\n", steps);
        }
        return "";
    }

    private int extractServings(JsonNode node) {
        JsonNode yieldNode = node.path("recipeYield");
        if (yieldNode.isTextual()) {
            Matcher m = Pattern.compile("(\\d+)").matcher(yieldNode.asText());
            if (m.find()) return Integer.parseInt(m.group(1));
        } else if (yieldNode.isArray() && !yieldNode.isEmpty()) {
            Matcher m = Pattern.compile("(\\d+)").matcher(yieldNode.get(0).asText());
            if (m.find()) return Integer.parseInt(m.group(1));
        } else if (yieldNode.isNumber()) {
            return yieldNode.asInt(1);
        }
        return 1;
    }

    private List<ImportedIngredientPreview> parseIngredientsWithRegex(List<String> rawIngredientStrings) {
        List<ImportedIngredientPreview> ingredients = new ArrayList<>();
        String currentSection = null;
        for (String raw : rawIngredientStrings) {
            String detectedSection = detectSection(raw);
            if (detectedSection != null) {
                currentSection = detectedSection;
                continue;
            }

            ImportedIngredientPreview parsed = parseIngredientText(raw);
            if (currentSection != null && (parsed.section() == null || parsed.section().isEmpty())) {
                parsed = new ImportedIngredientPreview(currentSection, parsed.name(), parsed.quantity(),
                        parsed.unit(), parsed.rawText(), parsed.prepNote());
            }
            ingredients.add(parsed);
        }
        return ingredients;
    }

    private String detectSection(String raw) {
        // "FROSTING:", "FOR THE SAUCE:", "Topping:"
        Matcher m = SECTION_HEADER.matcher(raw.trim());
        if (m.matches()) return titleCase(m.group(1));

        // "For the X:" pattern
        if (raw.trim().matches("(?i)^for\\s+(the\\s+)?\\w[\\w ]*:$")) {
            String section = raw.trim().replaceFirst("(?i)^for\\s+(the\\s+)?", "").replaceAll(":$", "").trim();
            return titleCase(section);
        }
        return null;
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String word : s.toLowerCase().split("\\s+")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    ImportedIngredientPreview parseIngredientText(String raw) {
        // Step 1: Unicode fractions
        String text = normalizeUnicodeFractions(raw);

        // Step 2: Normalize dash-fractions: "2-2/3" → "2 2/3"
        text = DASH_FRACTION.matcher(text).replaceAll("$1 $2");

        // Step 3: Handle dual unit formats: "3 1/3 cups/430 grams flour" → take first unit
        Matcher dualMatcher = DUAL_UNIT.matcher(text);
        if (dualMatcher.find()) {
            text = dualMatcher.group(1) + " " + dualMatcher.group(2) + " " + text.substring(dualMatcher.end());
        }

        // Step 4: Handle parenthetical measurements: "(28-ounce)" or "(14-1/2 ounces)"
        // If the text starts with qty + paren measurement, extract it as the primary measurement
        Matcher parenMatcher = PAREN_MEASUREMENT.matcher(text);
        String parenQty = null;
        String parenUnit = null;
        if (parenMatcher.find()) {
            parenQty = parenMatcher.group(1).replace("-", " ").replace("  ", " ");
            parenUnit = parenMatcher.group(2).toLowerCase();
            // Remove the parenthetical from text
            text = text.substring(0, parenMatcher.start()) + text.substring(parenMatcher.end());
            text = text.replaceAll("\\s{2,}", " ").trim();
        }

        // Step 5: Ranges — "3- to 5-pound" → "4 pound", "6 to 8 whole" → "7"
        Matcher rangeMatcher = RANGE_WITH_UNIT.matcher(text);
        if (rangeMatcher.find()) {
            BigDecimal low = parseFraction(rangeMatcher.group(1).trim());
            BigDecimal high = parseFraction(rangeMatcher.group(2).trim());
            BigDecimal mid = low.add(high).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            String midStr = mid.stripTrailingZeros().toPlainString();
            text = text.substring(0, rangeMatcher.start()) + midStr + " " + rangeMatcher.group(3) + text.substring(rangeMatcher.end());
        } else {
            Matcher simpleRange = RANGE_SIMPLE.matcher(text);
            if (simpleRange.find()) {
                BigDecimal low = parseFraction(simpleRange.group(1).trim());
                BigDecimal high = parseFraction(simpleRange.group(2).trim());
                BigDecimal mid = low.add(high).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                String midStr = mid.stripTrailingZeros().toPlainString();
                text = text.substring(0, simpleRange.start()) + midStr + text.substring(simpleRange.end());
            }
        }

        // Step 6: Number word prefixes: "One 3-pound" → "3 pound"
        Matcher numWord = NUMBER_WORD_PREFIX.matcher(text);
        if (numWord.find()) {
            String word = numWord.group(1).toLowerCase();
            if (NUMBER_WORDS.containsKey(word)) {
                String rest = text.substring(numWord.end()).trim();
                // If rest starts with a number+unit (like "3-pound"), use that
                if (rest.matches("^\\d+.*")) {
                    text = rest;
                } else {
                    // "One bay leaf" → "1 bay leaf"
                    text = NUMBER_WORDS.get(word) + " " + rest;
                }
            }
        }

        // Step 7: Extract quantity
        BigDecimal quantity = BigDecimal.ONE;
        String ingredientName = text;

        Matcher qtyMatcher = QTY_PATTERN.matcher(text);
        if (qtyMatcher.find()) {
            String qtyStr = qtyMatcher.group(1);
            quantity = parseFraction(qtyStr);
            ingredientName = text.substring(qtyMatcher.end()).trim();
        } else if (parenQty != null) {
            // Use parenthetical quantity if no leading quantity found
            quantity = parseFraction(parenQty);
        }

        // Step 8: Extract unit
        String unit = "WHOLE";
        Matcher unitMatcher = UNIT_PATTERN.matcher(text);
        if (unitMatcher.find()) {
            String matchedUnit = unitMatcher.group(1).toLowerCase().replaceAll("\\.$", "");
            unit = UNIT_ALIASES.getOrDefault(matchedUnit, "WHOLE");
            ingredientName = text.substring(unitMatcher.end()).trim();
        } else {
            // Try unit from the word immediately after the quantity
            String[] words = ingredientName.split("\\s+", 2);
            if (words.length >= 1 && !words[0].isEmpty()) {
                String candidate = words[0].toLowerCase().replaceAll("\\.$", "");
                // Exact match first
                String exactMatch = UNIT_ALIASES.get(candidate);
                if (exactMatch != null) {
                    unit = exactMatch;
                    ingredientName = words.length > 1 ? words[1].trim() : "";
                } else {
                    // Fuzzy match (but only for non-ambiguous candidates)
                    String fuzzyMatch = fuzzyMatchUnit(candidate);
                    if (fuzzyMatch != null) {
                        unit = fuzzyMatch;
                        ingredientName = words.length > 1 ? words[1].trim() : "";
                    }
                }
            }
        }

        // If we had a parenthetical measurement and no better unit was found, use it
        if (parenQty != null && "WHOLE".equals(unit) && parenUnit != null) {
            quantity = parseFraction(parenQty);
            String resolvedUnit = UNIT_ALIASES.get(parenUnit);
            if (resolvedUnit == null) resolvedUnit = UNIT_ALIASES.get(parenUnit.replaceAll("s$", ""));
            if (resolvedUnit != null) unit = resolvedUnit;
        }

        // Step 9: Strip modifiers from name: "plus 2 tablespoons", "minus 2 tablespoons"
        ingredientName = PLUS_MINUS_MOD.matcher(ingredientName).replaceAll("");

        // Step 10: Enhanced name cleanup
        ingredientName = ingredientName
                .replaceFirst("^/\\s*", "")
                .replaceFirst("^\\d+(?:[./]\\d+)?\\s+", "")  // strip second qty after separator
                .replaceAll("^of\\s+", "");

        // Strip parenthetical alt measurements and notes
        ingredientName = PAREN_ALT_MEASURE.matcher(ingredientName).replaceAll("");

        // Strip brand alternatives: "Diamond Crystal or 1/2 tsp. Morton kosher salt" → "kosher salt"
        ingredientName = BRAND_ALTERNATIVE.matcher(ingredientName).replaceAll("");

        // Strip "package" descriptor (the size was already extracted)
        ingredientName = PACKAGE_DESC.matcher(ingredientName).replaceAll("");

        // Strip "for <prep purpose>" clauses
        ingredientName = ingredientName.replaceAll("\\s+for\\s+(?:tossing|serving|garnish|frying|greasing|dusting|dipping|drizzling|topping|rolling)\\b.*", "");

        // Strip comma-separated prep notes
        ingredientName = ingredientName.replaceAll(",.*", "");

        // Strip trailing "to serve", "to taste", "(optional)"
        ingredientName = ingredientName.replaceAll("\\s+to\\s+(?:serve|taste)\\b.*", "");
        ingredientName = ingredientName.replaceAll("\\s*\\(optional\\)\\s*", "");

        // Clean up whitespace and leading/trailing punctuation
        ingredientName = ingredientName.replaceAll("\\s{2,}", " ").trim();
        ingredientName = ingredientName.replaceAll("^[\\s,;.]+|[\\s,;.]+$", "").trim();

        if (ingredientName.isEmpty()) {
            ingredientName = raw;
        }

        return new ImportedIngredientPreview(null, ingredientName, quantity, unit, raw, null);
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
                    return new BigDecimal(parts[0]).divide(new BigDecimal(parts[1]), 4, RoundingMode.HALF_UP);
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
        // Skip single-char candidates that could be ambiguous (avoid "g" fuzzy-matching "c")
        // Single-char exact matches are handled above; fuzzy needs 3+ chars
        if (candidate.length() < 3) return null;
        int maxDist = candidate.length() >= 5 ? 2 : 1;
        int bestDist = Integer.MAX_VALUE;
        String bestUnit = null;
        for (Map.Entry<String, String> entry : UNIT_ALIASES.entrySet()) {
            String alias = entry.getKey();
            if (Math.abs(alias.length() - candidate.length()) > maxDist) continue;
            int dist = editDistance(candidate, alias);
            if (dist <= maxDist && dist < bestDist) {
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
