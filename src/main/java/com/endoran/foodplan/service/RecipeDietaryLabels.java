package com.endoran.foodplan.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class RecipeDietaryLabels {

    private RecipeDietaryLabels() {}

    private static final Set<String> GLUTEN_KEYWORDS = Set.of(
            "bread", "pasta", "noodle", "couscous", "barley", "rye",
            "wheat", "panko", "crouton", "soy sauce", "seitan", "breadcrumb",
            "tortilla", "pita", "biscuit", "cracker", "pretzel", "orzo");

    private static final Set<String> DAIRY_KEYWORDS = Set.of(
            "cheese", "yogurt", "butter", "ghee", "whey", "sour cream",
            "ricotta", "mozzarella", "parmesan", "cheddar", "half-and-half",
            "mascarpone", "brie", "gruyere", "provolone", "gouda",
            "cream cheese", "cottage cheese", "heavy cream", "whipping cream");

    private static final Set<String> NUT_KEYWORDS = Set.of(
            "walnut", "pecan", "cashew", "pistachio", "hazelnut",
            "macadamia", "pine nut", "peanut");

    private static final Set<String> MEAT_KEYWORDS = Set.of(
            "chicken", "beef", "pork", "turkey", "sausage", "bacon",
            "lamb", "fish", "salmon", "shrimp", "anchovy", "prawn",
            "tuna", "crab", "lobster", "scallop", "clam", "mussel",
            "oyster", "ham", "veal", "venison", "duck", "bison",
            "chorizo", "pepperoni", "salami", "tilapia", "cod", "trout",
            "mahi", "halibut", "steak", "ground beef", "ground turkey",
            "chicken breast", "chicken thigh");

    private static final Set<String> ANIMAL_ONLY_KEYWORDS = Set.of(
            "egg", "honey", "gelatin", "lard");

    // Dairy-like words that are NOT dairy when preceded by these qualifiers
    private static final Pattern NON_DAIRY_MILK = Pattern.compile(
            "(coconut|almond|oat|soy|rice|cashew)\\s+(milk|cream|creamer)", Pattern.CASE_INSENSITIVE);

    // "flour" is gluten unless qualified
    private static final Pattern NON_GLUTEN_FLOUR = Pattern.compile(
            "(almond|rice|coconut|oat|chickpea|tapioca|corn|potato)\\s+flour", Pattern.CASE_INSENSITIVE);

    private static final Pattern PLAIN_FLOUR = Pattern.compile(
            "(?<!(almond|rice|coconut|oat|chickpea|tapioca|corn|potato)\\s)flour", Pattern.CASE_INSENSITIVE);

    // "almond" is a nut unless it's almond milk/flour/extract
    private static final Pattern ALMOND_AS_NUT = Pattern.compile(
            "almond(?!\\s+(milk|flour|extract))", Pattern.CASE_INSENSITIVE);

    // "milk" is dairy unless qualified
    private static final Pattern PLAIN_MILK = Pattern.compile(
            "(?<!(coconut|almond|oat|soy|rice|cashew)\\s)milk", Pattern.CASE_INSENSITIVE);

    private static final Pattern PLAIN_CREAM = Pattern.compile(
            "(?<!(coconut|almond|oat|soy|cashew)\\s)cream(?!\\s+of\\s+tartar)", Pattern.CASE_INSENSITIVE);

    public static List<String> compute(List<String> ingredientNames) {
        boolean hasGluten = false;
        boolean hasDairy = false;
        boolean hasNuts = false;
        boolean hasMeat = false;
        boolean hasAnimal = false;

        for (String name : ingredientNames) {
            String lower = name.toLowerCase();

            // Gluten check
            if (!hasGluten) {
                if (PLAIN_FLOUR.matcher(lower).find() && !NON_GLUTEN_FLOUR.matcher(lower).find()) {
                    hasGluten = true;
                } else {
                    for (String kw : GLUTEN_KEYWORDS) {
                        if (lower.contains(kw)) { hasGluten = true; break; }
                    }
                }
            }

            // Dairy check
            if (!hasDairy) {
                if (PLAIN_MILK.matcher(lower).find() || PLAIN_CREAM.matcher(lower).find()) {
                    hasDairy = true;
                } else {
                    for (String kw : DAIRY_KEYWORDS) {
                        if (lower.contains(kw)) { hasDairy = true; break; }
                    }
                }
            }

            // Nut check
            if (!hasNuts) {
                if (ALMOND_AS_NUT.matcher(lower).find()) {
                    hasNuts = true;
                } else {
                    for (String kw : NUT_KEYWORDS) {
                        if (lower.contains(kw)) { hasNuts = true; break; }
                    }
                }
            }

            // Meat check
            if (!hasMeat) {
                for (String kw : MEAT_KEYWORDS) {
                    if (lower.contains(kw)) { hasMeat = true; break; }
                }
            }

            // Animal-only check (egg, honey, etc. — not already covered by dairy/meat)
            if (!hasAnimal) {
                for (String kw : ANIMAL_ONLY_KEYWORDS) {
                    if (lower.contains(kw)) { hasAnimal = true; break; }
                }
            }
        }

        boolean containsAnimal = hasMeat || hasDairy || hasAnimal;

        List<String> labels = new ArrayList<>();
        if (!hasGluten) labels.add("Gluten Free");
        if (!hasDairy) labels.add("Dairy Free");
        if (!hasNuts) labels.add("Nut Free");
        if (!containsAnimal) labels.add("Vegan");
        else if (!hasMeat) labels.add("Vegetarian");
        return labels;
    }
}
