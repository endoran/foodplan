package com.endoran.foodplan.service;

import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;

import java.util.List;

public final class IngredientCategoryInference {

    public record InferredCategories(StorageCategory storage, GroceryCategory grocery) {}

    private record Rule(List<String> keywords, StorageCategory storage, GroceryCategory grocery) {}

    private static final List<Rule> RULES = List.of(
            new Rule(List.of("frozen"),
                    StorageCategory.FROZEN, GroceryCategory.FROZEN),
            new Rule(List.of("stock", "broth", "canned", "diced tomatoes", "tomato paste",
                    "tomato sauce", "coconut milk"),
                    StorageCategory.PANTRY, GroceryCategory.CANNED),
            new Rule(List.of("cream", "milk", "cheese", "yogurt", "butter", "sour cream"),
                    StorageCategory.REFRIGERATED, GroceryCategory.DAIRY),
            new Rule(List.of("chicken", "beef", "pork", "turkey", "sausage", "bacon",
                    "lamb", "fish", "salmon", "shrimp"),
                    StorageCategory.REFRIGERATED, GroceryCategory.MEAT),
            new Rule(List.of("cumin", "paprika", "oregano", "thyme", "rosemary", "cinnamon",
                    "nutmeg", "cayenne", "chili powder", "garlic powder", "onion powder",
                    "turmeric", "masala", "salt", "pepper flakes", "seasoning", "garam",
                    "red pepper", "black pepper", "white pepper"),
                    StorageCategory.SPICE_RACK, GroceryCategory.SPICES),
            new Rule(List.of("oil", "vinegar", "soy sauce", "mustard", "ketchup",
                    "mayonnaise", "honey"),
                    StorageCategory.PANTRY, GroceryCategory.OILS_CONDIMENTS),
            new Rule(List.of("flour", "sugar", "baking soda", "baking powder", "vanilla",
                    "cocoa"),
                    StorageCategory.PANTRY, GroceryCategory.BAKING),
            new Rule(List.of("cilantro", "basil", "parsley", "mint", "dill", "chives"),
                    StorageCategory.FRESH, GroceryCategory.PRODUCE),
            new Rule(List.of("garlic", "onion", "potato", "tomato", "banana", "avocado",
                    "shallot"),
                    StorageCategory.COUNTER, GroceryCategory.PRODUCE)
    );

    private static final InferredCategories DEFAULT =
            new InferredCategories(StorageCategory.PANTRY, GroceryCategory.PRODUCE);

    private IngredientCategoryInference() {}

    public static InferredCategories infer(String ingredientName) {
        String lower = ingredientName.toLowerCase();
        for (Rule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (lower.contains(keyword)) {
                    return new InferredCategories(rule.storage(), rule.grocery());
                }
            }
        }
        return DEFAULT;
    }
}
