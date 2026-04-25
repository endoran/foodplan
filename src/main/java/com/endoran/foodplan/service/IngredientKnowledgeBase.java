package com.endoran.foodplan.service;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.endoran.foodplan.model.DietaryTag.*;
import static com.endoran.foodplan.model.GroceryCategory.*;
import static com.endoran.foodplan.model.StorageCategory.*;

public final class IngredientKnowledgeBase {

    public record IngredientProfile(
            StorageCategory storage,
            GroceryCategory grocery,
            Set<DietaryTag> dietaryTags
    ) {}

    private static final Map<String, IngredientProfile> PROFILES;

    static {
        Map<String, IngredientProfile> m = new HashMap<>();

        // ===== DAIRY =====
        profile(m, "Butter", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Cheddar Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Cottage Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Cream Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Eggs", REFRIGERATED, DAIRY, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Gruyere Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Half and Half", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Heavy Cream", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Monterey Jack Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Mozzarella Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Parmesan Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Pepper Jack Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Plain Yogurt", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Ricotta Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Sour Cream", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Swiss Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Whole Milk", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Evaporated Milk", PANTRY, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Sweetened Condensed Milk", PANTRY, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Whipped Cream", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Goat Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Blue Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Feta Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Provolone Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Colby Jack Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Brie Cheese", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Mascarpone", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Buttermilk", REFRIGERATED, DAIRY, GLUTEN_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Ghee", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, NUT_FREE, VEGETARIAN);

        // ===== PROTEINS — MEAT =====
        profile(m, "Bacon", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Chicken Breast", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Chicken Thighs", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Chicken", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Cooked Chicken Breast", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Ground Beef", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Ground Pork", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Ground Turkey", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Italian Sausage", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Pork Chops", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Salmon Fillet", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Shrimp", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Tofu", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Whole Chicken", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Whole Young Chicken", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Ham", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Turkey Breast", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Lamb Chops", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Ground Lamb", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Stew Meat", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Flank Steak", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Ribeye Steak", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Sirloin Steak", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Chuck Roast", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Pork Tenderloin", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Pork Shoulder", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Baby Back Ribs", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Chicken Wings", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Chicken Drumsticks", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Tilapia", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Cod", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Tuna Steak", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Crab Meat", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Scallops", REFRIGERATED, MEAT, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Anchovy Paste", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Canned Tuna", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Pepperoni", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Prosciutto", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Salami", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);

        // ===== PRODUCE — FRESH =====
        profile(m, "Avocado", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Bell Pepper", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Broccoli", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cabbage", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Carrot", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cauliflower", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Celery", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cherry Tomatoes", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Corn", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cucumber", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Eggplant", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Green Beans", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Green Onion", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Jalapeno", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Kale", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Mushrooms", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Rainbow Bell Peppers", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Red Onion", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Roma Tomato", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Romaine Lettuce", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Spinach", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sweet Potato", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tomatoes", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Yellow Onion", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Zucchini", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Asparagus", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Brussels Sprouts", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Butternut Squash", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Acorn Squash", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Snap Peas", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Snow Peas", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Artichoke", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Beets", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Bok Choy", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Radishes", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Turnips", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fennel", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Leeks", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Okra", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Diced Okra", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Iceberg Lettuce", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Mixed Greens", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Arugula", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Watercress", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== PRODUCE — COUNTER =====
        profile(m, "Garlic", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Russet Potato", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Ginger", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Shallot", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== PRODUCE — FRUITS =====
        profile(m, "Lemon", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Lime", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Apple", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Banana", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Orange", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Strawberries", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Blueberries", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Raspberries", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Grapes", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pineapple", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Mango", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Peach", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pear", COUNTER, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== FRESH HERBS =====
        profile(m, "Cilantro", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Basil", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Dill", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Parsley", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Rosemary", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Thyme", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Mint", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Sage", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fresh Chives", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Lemongrass", FRESH, PRODUCE, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== SPICES & SEASONINGS =====
        profile(m, "Bay Leaves", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cayenne Pepper", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Chili Powder", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Dried Basil", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Dried Oregano", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Garam Masala", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Garlic Powder", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Black Pepper", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Cinnamon", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Cumin", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Ginger", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Nutmeg", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Turmeric", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Italian Seasoning", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Onion Powder", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Paprika", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Powdered Mustard", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Red Pepper Flakes", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Smoked Paprika", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Kosher Salt", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Salt", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Sea Salt", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Mineral Salt", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Celery Salt", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Coriander", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cumin Seeds", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Curry Powder", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Five Spice Powder", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Allspice", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Cardamom", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground Cloves", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ground White Pepper", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Mustard Seeds", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Saffron", SPICE_RACK, SPICES, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Taco Seasoning", SPICE_RACK, SPICES, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Old Bay Seasoning", SPICE_RACK, SPICES, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Ranch Seasoning", SPICE_RACK, SPICES, NUT_FREE);
        profile(m, "Everything Bagel Seasoning", SPICE_RACK, SPICES, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== OILS & CONDIMENTS =====
        profile(m, "Olive Oil", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Vegetable Oil", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sesame Oil", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Coconut Oil", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Avocado Oil", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Soy Sauce", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fish Sauce", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Worcestershire Sauce", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE);
        profile(m, "Hot Sauce", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sriracha", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Dijon Mustard", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Yellow Mustard", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ketchup", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Mayonnaise", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Apple Cider Vinegar", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "White Vinegar", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Red Wine Vinegar", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Rice Vinegar", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Balsamic Vinegar", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Honey", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Maple Syrup", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Lemon Juice", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Lime Juice", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Oyster Sauce", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE);
        profile(m, "Hoisin Sauce", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Teriyaki Sauce", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE);
        profile(m, "BBQ Sauce", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Salsa", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tahini", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Miso Paste", PANTRY, OILS_CONDIMENTS, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tomato Ketchup", PANTRY, OILS_CONDIMENTS, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pesto", REFRIGERATED, OILS_CONDIMENTS, NUT_FREE, VEGETARIAN);

        // ===== PANTRY — BAKING =====
        profile(m, "All-Purpose Flour", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Bread Flour", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cake Flour", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Whole Wheat Flour", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Almond Flour", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Coconut Flour", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Granulated Sugar", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Brown Sugar", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Powdered Sugar", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Baking Soda", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Baking Powder", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Vanilla Extract", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Almond Extract", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cornstarch", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cocoa Powder", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Chocolate Chips", PANTRY, BAKING, NUT_FREE, VEGETARIAN);
        profile(m, "Yeast", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Active Dry Yeast", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cream of Tartar", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Gelatin", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, SUGAR_FREE);
        profile(m, "Food Coloring", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== PANTRY — GRAINS & PASTA =====
        profile(m, "White Rice", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Brown Rice", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Jasmine Rice", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Basmati Rice", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Arborio Rice", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Quinoa", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Couscous", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Oats", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pasta", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Spaghetti", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Elbow Macaroni", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Penne", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Linguine", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Fettuccine", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Lasagna Noodles", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Egg Noodles", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Rice Noodles", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tortillas", PANTRY, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Flour Tortillas", PANTRY, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Corn Tortillas", PANTRY, BAKERY, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Panko Breadcrumbs", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Breadcrumbs", PANTRY, BAKING, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Nutritional Yeast", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== CANNED =====
        profile(m, "Black Beans", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Kidney Beans", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pinto Beans", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Chickpeas", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Chicken Broth", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Beef Broth", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Vegetable Broth", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Coconut Milk", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Crushed Tomatoes", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Diced Tomatoes", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tomato Paste", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tomato Sauce", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Enchilada Sauce", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Green Chiles", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Roasted Red Peppers", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Capers", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Olives", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sun-Dried Tomatoes", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Artichoke Hearts", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Canned Corn", PANTRY, CANNED, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Marinara Sauce", PANTRY, CANNED, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== NUTS & SEEDS =====
        profile(m, "Almonds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sliced Almonds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Cashew Nuts", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Peanuts", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Peanut Butter", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pecans", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pine Nuts", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sesame Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sunflower Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pumpkin Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Walnuts", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, VEGAN, VEGETARIAN);
        profile(m, "Chia Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Flax Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Hemp Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Poppy Seeds", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Coconut Flakes", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Shredded Coconut", PANTRY, BAKING, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== FROZEN =====
        profile(m, "Frozen Berries", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Corn", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Mixed Vegetables", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Peas", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Spinach", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Broccoli", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Shrimp", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Frozen Chicken Tenders", StorageCategory.FROZEN, GroceryCategory.FROZEN, DAIRY_FREE, NUT_FREE);
        profile(m, "Frozen Pizza Dough", StorageCategory.FROZEN, GroceryCategory.FROZEN, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Frozen Pie Crust", StorageCategory.FROZEN, GroceryCategory.FROZEN, NUT_FREE, VEGETARIAN);
        profile(m, "Frozen Fruit", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Ice Cream", StorageCategory.FROZEN, GroceryCategory.FROZEN, GLUTEN_FREE, NUT_FREE, VEGETARIAN);

        // ===== BAKERY =====
        profile(m, "Bread", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Hamburger Buns", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Hot Dog Buns", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Pita Bread", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Naan", COUNTER, BAKERY, NUT_FREE, VEGETARIAN);
        profile(m, "French Bread", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sourdough Bread", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Croissants", COUNTER, BAKERY, NUT_FREE, VEGETARIAN);
        profile(m, "English Muffins", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Bagels", COUNTER, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Croutons", PANTRY, BAKERY, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Pizza Dough", REFRIGERATED, BAKERY, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== HOUSEHOLD & WATER =====
        profile(m, "Water", COUNTER, HOUSEHOLD, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Ice", COUNTER, HOUSEHOLD, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);

        // ===== BEVERAGES =====
        profile(m, "Green Tea Bags", PANTRY, HOUSEHOLD, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Black Tea Bags", PANTRY, HOUSEHOLD, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Coffee", PANTRY, HOUSEHOLD, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Matcha Powder", PANTRY, HOUSEHOLD, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);

        // ===== HEALTH SUPPLEMENTS =====
        profile(m, "Baobab Boost Powder", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Integral Collagen", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, SUGAR_FREE);
        profile(m, "Just Gelatin", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, SUGAR_FREE);
        profile(m, "Pure Stevia Extract Powder", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN, SUGAR_FREE);
        profile(m, "Sunflower Lecithin", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Protein Powder", PANTRY, BULK, GLUTEN_FREE, NUT_FREE);
        profile(m, "Whey Protein", PANTRY, BULK, GLUTEN_FREE, NUT_FREE);
        profile(m, "Collagen Peptides", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, SUGAR_FREE);
        profile(m, "MCT Oil", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Apple Cider Vinegar Gummies", PANTRY, BULK, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== ETHNIC / SPECIALTY =====
        profile(m, "Gochujang", PANTRY, ETHNIC, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sambal Oelek", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Curry Paste", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Harissa", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Chipotle Peppers in Adobo", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Wonton Wrappers", REFRIGERATED, ETHNIC, DAIRY_FREE, NUT_FREE, VEGETARIAN);
        profile(m, "Spring Roll Wrappers", PANTRY, ETHNIC, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Coconut Cream", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Tamarind Paste", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Nori Sheets", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Mirin", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Sake", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Dried Lentils", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Red Lentils", PANTRY, ETHNIC, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        // ===== DELI =====
        profile(m, "Deli Turkey", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Deli Ham", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE);
        profile(m, "Hummus", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);
        profile(m, "Guacamole", REFRIGERATED, DELI, GLUTEN_FREE, DAIRY_FREE, NUT_FREE, VEGAN, VEGETARIAN);

        PROFILES = Map.copyOf(m);
    }

    private IngredientKnowledgeBase() {}

    public static Optional<IngredientProfile> lookup(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) return Optional.empty();
        return Optional.ofNullable(PROFILES.get(ingredientName.toLowerCase()));
    }

    public static int size() {
        return PROFILES.size();
    }

    private static void profile(Map<String, IngredientProfile> map,
                                String canonicalName,
                                StorageCategory storage,
                                GroceryCategory grocery,
                                DietaryTag... tags) {
        map.put(canonicalName.toLowerCase(),
                new IngredientProfile(storage, grocery, Set.of(tags)));
    }
}
