package com.endoran.foodplan.service;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;

import java.util.List;
import java.util.Set;

import static com.endoran.foodplan.model.DietaryTag.*;

public final class IngredientCategoryInference {

    public record InferredCategories(StorageCategory storage, GroceryCategory grocery,
                                     Set<DietaryTag> dietaryTags) {}

    private record Rule(List<String> keywords, StorageCategory storage, GroceryCategory grocery,
                        Set<DietaryTag> dietaryTags) {}

    private static final Set<DietaryTag> ALL_CLEAR = Set.of(GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
    private static final Set<DietaryTag> MEAT_TAGS = Set.of(GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
    private static final Set<DietaryTag> DAIRY_TAGS = Set.of(GLUTEN_FREE, NUT_FREE, VEGETARIAN);

    private static final List<Rule> RULES = List.of(
            new Rule(List.of("frozen"),
                    StorageCategory.FROZEN, GroceryCategory.FROZEN, ALL_CLEAR),
            new Rule(List.of("stock", "broth", "canned", "diced tomatoes", "tomato paste",
                    "tomato sauce", "coconut milk"),
                    StorageCategory.PANTRY, GroceryCategory.CANNED, ALL_CLEAR),
            new Rule(List.of("cream", "milk", "cheese", "yogurt", "butter", "sour cream"),
                    StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, DAIRY_TAGS),
            new Rule(List.of("chicken", "beef", "pork", "turkey", "sausage", "bacon",
                    "lamb", "fish", "salmon", "shrimp", "tilapia", "cod"),
                    StorageCategory.REFRIGERATED, GroceryCategory.MEAT, MEAT_TAGS),
            new Rule(List.of("cumin", "paprika", "oregano", "thyme", "rosemary", "cinnamon",
                    "nutmeg", "cayenne", "chili powder", "garlic powder", "onion powder",
                    "turmeric", "masala", "salt", "pepper flakes", "seasoning", "garam",
                    "red pepper", "black pepper", "white pepper", "curry", "allspice",
                    "cardamom", "cloves", "coriander", "mustard seed"),
                    StorageCategory.SPICE_RACK, GroceryCategory.SPICES, ALL_CLEAR),
            new Rule(List.of("oil", "vinegar", "soy sauce", "mustard", "ketchup",
                    "mayonnaise", "honey", "worcestershire", "fish sauce", "hot sauce",
                    "sriracha", "bbq sauce", "hoisin", "teriyaki", "anchovy paste",
                    "tahini", "miso", "ghee", "lemon juice", "lime juice"),
                    StorageCategory.PANTRY, GroceryCategory.OILS_CONDIMENTS, ALL_CLEAR),
            new Rule(List.of("flour", "sugar", "baking soda", "baking powder", "vanilla",
                    "cocoa", "cornstarch", "breadcrumb", "panko", "yeast",
                    "pasta", "spaghetti", "penne", "macaroni", "noodle", "rice",
                    "oats", "quinoa"),
                    StorageCategory.PANTRY, GroceryCategory.BAKING, ALL_CLEAR),
            new Rule(List.of("almond", "walnut", "pecan", "peanut", "cashew", "pistachio",
                    "pine nut", "hazelnut", "macadamia"),
                    StorageCategory.PANTRY, GroceryCategory.BULK, Set.of(GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN)),
            new Rule(List.of("cilantro", "basil", "parsley", "mint", "dill", "chives",
                    "sage", "lemongrass"),
                    StorageCategory.FRESH, GroceryCategory.PRODUCE, ALL_CLEAR),
            new Rule(List.of("lettuce", "spinach", "kale", "arugula", "cabbage",
                    "broccoli", "cauliflower", "zucchini", "bell pepper", "cucumber",
                    "celery", "carrot", "mushroom", "asparagus", "green bean",
                    "brussels sprout", "bok choy", "radish", "leek", "okra"),
                    StorageCategory.FRESH, GroceryCategory.PRODUCE, ALL_CLEAR),
            new Rule(List.of("garlic", "onion", "potato", "tomato", "banana", "avocado",
                    "shallot", "ginger", "lemon", "lime", "apple", "orange"),
                    StorageCategory.COUNTER, GroceryCategory.PRODUCE, ALL_CLEAR),
            new Rule(List.of("bread", "bun", "tortilla", "pita", "naan", "croissant",
                    "muffin", "bagel"),
                    StorageCategory.COUNTER, GroceryCategory.BAKERY, Set.of(DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN)),
            new Rule(List.of("tea", "coffee", "matcha"),
                    StorageCategory.PANTRY, GroceryCategory.HOUSEHOLD, ALL_CLEAR)
    );

    private static final InferredCategories DEFAULT =
            new InferredCategories(StorageCategory.PANTRY, GroceryCategory.PRODUCE, ALL_CLEAR);

    private IngredientCategoryInference() {}

    public static InferredCategories infer(String ingredientName) {
        var profile = IngredientKnowledgeBase.lookup(ingredientName);
        if (profile.isPresent()) {
            var p = profile.get();
            return new InferredCategories(p.storage(), p.grocery(), p.dietaryTags());
        }

        String lower = ingredientName.toLowerCase();
        for (Rule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (lower.contains(keyword)) {
                    return new InferredCategories(rule.storage(), rule.grocery(), rule.dietaryTags());
                }
            }
        }
        return DEFAULT;
    }
}
