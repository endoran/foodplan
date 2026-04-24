package com.endoran.foodplan.service;

import java.util.HashMap;
import java.util.Map;

public final class IngredientAliasDictionary {

    private static final Map<String, String> ALIASES;

    static {
        Map<String, String> m = new HashMap<>();

        // --- Dairy ---
        alias(m, "Parmesan Cheese", "parmigiano-reggiano", "parmigiano reggiano", "parmesan",
                "parm cheese", "grated parmesan", "shredded parmesan");
        alias(m, "Heavy Cream", "heavy whipping cream", "whipping cream");
        alias(m, "Cream Cheese", "cream cheese spread", "neufchatel");
        alias(m, "Sour Cream", "soured cream");
        alias(m, "Butter", "unsalted butter", "salted butter", "sweet cream butter");
        alias(m, "Whole Milk", "milk", "whole milk");
        alias(m, "Cheddar Cheese", "cheddar", "sharp cheddar", "mild cheddar");
        alias(m, "Mozzarella Cheese", "mozzarella", "fresh mozzarella", "shredded mozzarella");
        alias(m, "Monterey Jack Cheese", "monterey jack");
        alias(m, "Pepper Jack Cheese", "pepper jack");
        alias(m, "Swiss Cheese", "swiss");
        alias(m, "Gruyere Cheese", "gruyere", "gruyère");
        alias(m, "Ricotta Cheese", "ricotta");
        alias(m, "Cottage Cheese", "cottage cheese");
        alias(m, "Plain Yogurt", "yogurt", "plain greek yogurt", "greek yogurt");
        alias(m, "Eggs", "egg", "large egg", "large eggs", "whole egg", "whole eggs");
        alias(m, "Half and Half", "half & half", "half-and-half");

        // --- Proteins ---
        alias(m, "Ground Beef", "hamburger meat", "ground chuck", "lean ground beef",
                "hamburger", "minced beef", "beef mince");
        alias(m, "Chicken Breast", "boneless skinless chicken breast", "boneless chicken breast",
                "skinless chicken breast", "chicken breasts");
        alias(m, "Chicken Thighs", "boneless skinless chicken thighs", "boneless chicken thighs",
                "chicken thigh");
        alias(m, "Whole Chicken", "roasting chicken", "whole fryer");
        alias(m, "Bacon", "bacon strips", "sliced bacon", "thick cut bacon");
        alias(m, "Italian Sausage", "italian sausage links", "sweet italian sausage",
                "hot italian sausage", "mild italian sausage");
        alias(m, "Ground Turkey", "lean ground turkey", "turkey mince");
        alias(m, "Ground Pork", "pork mince");
        alias(m, "Pork Chops", "pork chop", "bone-in pork chops", "boneless pork chops");
        alias(m, "Salmon Fillet", "salmon", "salmon filet", "fresh salmon");
        alias(m, "Shrimp", "prawns", "raw shrimp", "large shrimp", "jumbo shrimp");
        alias(m, "Tofu", "firm tofu", "extra firm tofu");

        // --- Produce ---
        alias(m, "Green Onion", "scallion", "scallions", "spring onion", "spring onions",
                "green onions");
        alias(m, "Cilantro", "fresh cilantro", "coriander leaves", "fresh coriander");
        alias(m, "Garlic", "fresh garlic", "garlic cloves", "garlic clove", "cloves garlic",
                "clove garlic", "minced garlic");
        alias(m, "Yellow Onion", "onion", "medium onion", "large onion", "onions",
                "yellow onions", "cooking onion");
        alias(m, "Red Onion", "red onions");
        alias(m, "Bell Pepper", "green bell pepper", "red bell pepper", "bell peppers",
                "green pepper", "red pepper");
        alias(m, "Jalapeno", "jalapeño", "jalapeno pepper", "jalapeño pepper",
                "jalapenos", "jalapeños");
        alias(m, "Fresh Ginger", "ginger", "ginger root", "gingerroot");
        alias(m, "Lemon", "lemons", "fresh lemon");
        alias(m, "Lime", "limes", "fresh lime");
        alias(m, "Roma Tomato", "roma tomatoes", "plum tomato", "plum tomatoes");
        alias(m, "Cherry Tomatoes", "grape tomatoes", "cherry tomato");
        alias(m, "Russet Potato", "potato", "potatoes", "russet potatoes", "baking potato");
        alias(m, "Sweet Potato", "sweet potatoes", "yam", "yams");
        alias(m, "Celery", "celery stalks", "celery stalk", "celery ribs");
        alias(m, "Carrot", "carrots", "large carrot");
        alias(m, "Broccoli", "broccoli florets", "broccoli crowns");
        alias(m, "Spinach", "fresh spinach", "baby spinach");
        alias(m, "Romaine Lettuce", "romaine", "romaine hearts");
        alias(m, "Avocado", "avocados", "ripe avocado");
        alias(m, "Mushrooms", "mushroom", "white mushrooms", "button mushrooms",
                "cremini mushrooms", "baby bella mushrooms");
        alias(m, "Corn", "sweet corn", "corn kernels", "corn on the cob");
        alias(m, "Cabbage", "green cabbage", "head of cabbage");
        alias(m, "Zucchini", "zucchini squash", "courgette");
        alias(m, "Fresh Basil", "basil", "basil leaves", "sweet basil");
        alias(m, "Fresh Parsley", "parsley", "flat leaf parsley", "italian parsley",
                "flat-leaf parsley", "curly parsley");
        alias(m, "Fresh Dill", "dill", "dill weed");
        alias(m, "Fresh Thyme", "thyme", "thyme sprigs");
        alias(m, "Fresh Rosemary", "rosemary", "rosemary sprigs");

        // --- Spices & Seasonings ---
        alias(m, "Garlic Powder", "granulated garlic", "dehydrated garlic");
        alias(m, "Onion Powder", "dehydrated onion");
        alias(m, "Chili Powder", "chile powder");
        alias(m, "Ground Cumin", "cumin", "cumin powder");
        alias(m, "Smoked Paprika", "pimenton");
        alias(m, "Paprika", "sweet paprika", "hungarian paprika");
        alias(m, "Ground Cinnamon", "cinnamon", "cinnamon powder");
        alias(m, "Ground Nutmeg", "nutmeg", "nutmeg powder");
        alias(m, "Cayenne Pepper", "cayenne", "ground cayenne", "ground red pepper");
        alias(m, "Red Pepper Flakes", "crushed red pepper", "red chili flakes",
                "crushed red pepper flakes");
        alias(m, "Italian Seasoning", "italian herb blend", "italian herbs");
        alias(m, "Bay Leaves", "bay leaf", "dried bay leaves");
        alias(m, "Dried Oregano", "oregano");
        alias(m, "Dried Basil", "basil flakes");
        alias(m, "Ground Ginger", "ginger powder", "dried ginger");
        alias(m, "Ground Turmeric", "turmeric", "turmeric powder");
        alias(m, "Garam Masala", "garam masala powder");
        alias(m, "Ground Black Pepper", "black pepper", "pepper", "ground pepper",
                "freshly ground pepper", "freshly ground black pepper", "cracked black pepper");
        // Note: salt variants are intentionally DISTINCT — do NOT alias across them
        // kosher salt, sea salt, table salt, pink salt, ice cream salt are all different

        // --- Pantry ---
        alias(m, "All-Purpose Flour", "ap flour", "plain flour", "all purpose flour",
                "flour", "white flour", "unbleached flour");
        alias(m, "Bread Flour", "strong flour", "high gluten flour");
        alias(m, "Granulated Sugar", "sugar", "white sugar", "cane sugar");
        alias(m, "Brown Sugar", "light brown sugar", "dark brown sugar", "packed brown sugar");
        alias(m, "Powdered Sugar", "confectioners sugar", "confectioner's sugar",
                "icing sugar", "10x sugar");
        alias(m, "Baking Soda", "bicarbonate of soda", "bicarb");
        alias(m, "Baking Powder", "double acting baking powder");
        alias(m, "Vanilla Extract", "vanilla", "pure vanilla extract", "pure vanilla");
        alias(m, "Cornstarch", "corn starch", "cornflour");
        alias(m, "Panko Breadcrumbs", "panko", "panko bread crumbs");
        alias(m, "Breadcrumbs", "bread crumbs", "dried breadcrumbs", "plain breadcrumbs");
        alias(m, "White Rice", "rice", "long grain rice", "long-grain white rice");
        alias(m, "Jasmine Rice", "jasmine");
        alias(m, "Pasta", "dried pasta", "dry pasta");
        alias(m, "Spaghetti", "spaghetti noodles", "spaghetti pasta");
        alias(m, "Elbow Macaroni", "macaroni", "elbow pasta");
        alias(m, "Penne", "penne pasta", "penne rigate");
        alias(m, "Cocoa Powder", "unsweetened cocoa powder", "cocoa", "unsweetened cocoa");

        // --- Oils & Condiments ---
        alias(m, "Olive Oil", "extra virgin olive oil", "evoo", "extra-virgin olive oil",
                "light olive oil");
        alias(m, "Vegetable Oil", "canola oil", "neutral oil", "cooking oil");
        alias(m, "Sesame Oil", "toasted sesame oil", "dark sesame oil");
        alias(m, "Soy Sauce", "soya sauce", "shoyu", "tamari");
        alias(m, "Worcestershire Sauce", "worcestershire", "lea & perrins");
        alias(m, "Hot Sauce", "hot pepper sauce", "tabasco", "louisiana hot sauce");
        alias(m, "Dijon Mustard", "dijon");
        alias(m, "Yellow Mustard", "prepared mustard", "american mustard");
        alias(m, "Ketchup", "catsup", "tomato ketchup");
        alias(m, "Mayonnaise", "mayo");
        alias(m, "Apple Cider Vinegar", "cider vinegar", "acv");
        alias(m, "White Vinegar", "distilled white vinegar", "distilled vinegar");
        alias(m, "Red Wine Vinegar", "red wine vin");
        alias(m, "Rice Vinegar", "rice wine vinegar", "seasoned rice vinegar");
        alias(m, "Honey", "raw honey", "pure honey");
        alias(m, "Maple Syrup", "pure maple syrup");

        // --- Canned ---
        alias(m, "Diced Tomatoes", "canned diced tomatoes", "diced tomatoes (canned)",
                "petite diced tomatoes");
        alias(m, "Crushed Tomatoes", "canned crushed tomatoes");
        alias(m, "Tomato Paste", "tomato paste (canned)");
        alias(m, "Tomato Sauce", "canned tomato sauce");
        alias(m, "Chicken Broth", "chicken stock", "chicken bone broth");
        alias(m, "Beef Broth", "beef stock", "beef bone broth");
        alias(m, "Vegetable Broth", "vegetable stock", "veggie broth", "veggie stock");
        alias(m, "Coconut Milk", "canned coconut milk", "full fat coconut milk",
                "coconut milk (canned)");
        alias(m, "Black Beans", "canned black beans", "black beans (canned)");
        alias(m, "Kidney Beans", "canned kidney beans", "red kidney beans");
        alias(m, "Pinto Beans", "canned pinto beans");
        alias(m, "Chickpeas", "garbanzo beans", "canned chickpeas");

        // --- Nuts & Seeds ---
        alias(m, "Almonds", "whole almonds", "raw almonds");
        alias(m, "Sliced Almonds", "slivered almonds");
        alias(m, "Walnuts", "walnut pieces", "chopped walnuts");
        alias(m, "Pecans", "pecan pieces", "chopped pecans", "pecan halves");
        alias(m, "Peanuts", "roasted peanuts", "dry roasted peanuts");
        alias(m, "Peanut Butter", "creamy peanut butter", "smooth peanut butter");
        alias(m, "Sesame Seeds", "toasted sesame seeds", "white sesame seeds");

        // --- Frozen ---
        alias(m, "Frozen Peas", "frozen green peas", "peas (frozen)");
        alias(m, "Frozen Corn", "frozen sweet corn", "corn (frozen)");
        alias(m, "Frozen Spinach", "frozen chopped spinach", "spinach (frozen)");
        alias(m, "Frozen Mixed Vegetables", "frozen mixed veggies", "frozen vegetables");
        alias(m, "Frozen Berries", "frozen mixed berries");

        ALIASES = Map.copyOf(m);
    }

    private IngredientAliasDictionary() {}

    public static String resolve(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) return normalizedName;
        return ALIASES.getOrDefault(normalizedName.toLowerCase(), normalizedName);
    }

    public static String resolveAndNormalize(String rawName) {
        String normalized = IngredientNameNormalizer.normalize(rawName);
        return resolve(normalized);
    }

    private static void alias(Map<String, String> map, String canonical, String... aliases) {
        for (String a : aliases) {
            map.put(a.toLowerCase(), canonical);
        }
    }
}
