package com.endoran.foodplan.service;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.service.IngredientKnowledgeBase.IngredientProfile;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IngredientKnowledgeBaseTest {

    @Test
    void parmesanCheeseIsDairy() {
        Optional<IngredientProfile> profile = IngredientKnowledgeBase.lookup("Parmesan Cheese");
        assertTrue(profile.isPresent());
        assertEquals(StorageCategory.REFRIGERATED, profile.get().storage());
        assertEquals(GroceryCategory.DAIRY, profile.get().grocery());
        assertTrue(profile.get().dietaryTags().contains(DietaryTag.GLUTEN_FREE));
        assertTrue(profile.get().dietaryTags().contains(DietaryTag.VEGETARIAN));
        assertFalse(profile.get().dietaryTags().contains(DietaryTag.VEGAN));
    }

    @Test
    void unknownIngredientReturnsEmpty() {
        assertTrue(IngredientKnowledgeBase.lookup("Pixie Dust").isEmpty());
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertTrue(IngredientKnowledgeBase.lookup("PARMESAN CHEESE").isPresent());
        assertTrue(IngredientKnowledgeBase.lookup("parmesan cheese").isPresent());
        assertTrue(IngredientKnowledgeBase.lookup("Parmesan Cheese").isPresent());
    }

    @Test
    void nullAndBlankReturnEmpty() {
        assertTrue(IngredientKnowledgeBase.lookup(null).isEmpty());
        assertTrue(IngredientKnowledgeBase.lookup("").isEmpty());
        assertTrue(IngredientKnowledgeBase.lookup("   ").isEmpty());
    }

    // Staging misclassification fixes
    @Test
    void anchovyPasteIsOilsCondiments() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Anchovy Paste").orElseThrow();
        assertEquals(StorageCategory.PANTRY, p.storage());
        assertEquals(GroceryCategory.OILS_CONDIMENTS, p.grocery());
    }

    @Test
    void gheeIsOilsCondiments() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Ghee").orElseThrow();
        assertEquals(StorageCategory.PANTRY, p.storage());
        assertEquals(GroceryCategory.OILS_CONDIMENTS, p.grocery());
    }

    @Test
    void groundBlackPepperIsSpices() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Ground Black Pepper").orElseThrow();
        assertEquals(StorageCategory.SPICE_RACK, p.storage());
        assertEquals(GroceryCategory.SPICES, p.grocery());
    }

    @Test
    void worcestershireSauceIsOilsCondiments() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Worcestershire Sauce").orElseThrow();
        assertEquals(StorageCategory.PANTRY, p.storage());
        assertEquals(GroceryCategory.OILS_CONDIMENTS, p.grocery());
    }

    @Test
    void romaineLettuceIsFreshProduce() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Romaine Lettuce").orElseThrow();
        assertEquals(StorageCategory.FRESH, p.storage());
        assertEquals(GroceryCategory.PRODUCE, p.grocery());
    }

    @Test
    void rainbowBellPeppersIsFreshProduce() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Rainbow Bell Peppers").orElseThrow();
        assertEquals(StorageCategory.FRESH, p.storage());
        assertEquals(GroceryCategory.PRODUCE, p.grocery());
    }

    @Test
    void lemonJuiceIsOilsCondiments() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Lemon Juice").orElseThrow();
        assertEquals(StorageCategory.PANTRY, p.storage());
        assertEquals(GroceryCategory.OILS_CONDIMENTS, p.grocery());
    }

    @Test
    void mineralSaltIsSpices() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Mineral Salt").orElseThrow();
        assertEquals(StorageCategory.SPICE_RACK, p.storage());
        assertEquals(GroceryCategory.SPICES, p.grocery());
    }

    // Dietary tag correctness
    @Test
    void chickenBreastIsNotVegetarian() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Chicken Breast").orElseThrow();
        assertFalse(p.dietaryTags().contains(DietaryTag.VEGAN));
        assertFalse(p.dietaryTags().contains(DietaryTag.VEGETARIAN));
        assertTrue(p.dietaryTags().contains(DietaryTag.GLUTEN_FREE));
        assertTrue(p.dietaryTags().contains(DietaryTag.DAIRY_FREE));
    }

    @Test
    void oliveOilIsFullyPlantBased() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Olive Oil").orElseThrow();
        assertTrue(p.dietaryTags().contains(DietaryTag.VEGAN));
        assertTrue(p.dietaryTags().contains(DietaryTag.VEGETARIAN));
        assertTrue(p.dietaryTags().contains(DietaryTag.GLUTEN_FREE));
        assertTrue(p.dietaryTags().contains(DietaryTag.DAIRY_FREE));
        assertTrue(p.dietaryTags().contains(DietaryTag.NUT_FREE));
    }

    @Test
    void almondFlourIsNotNutFree() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Almond Flour").orElseThrow();
        assertFalse(p.dietaryTags().contains(DietaryTag.NUT_FREE));
        assertTrue(p.dietaryTags().contains(DietaryTag.GLUTEN_FREE));
    }

    @Test
    void eggsAreDairyFree() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Eggs").orElseThrow();
        assertTrue(p.dietaryTags().contains(DietaryTag.DAIRY_FREE));
        assertFalse(p.dietaryTags().contains(DietaryTag.VEGAN));
        assertTrue(p.dietaryTags().contains(DietaryTag.VEGETARIAN));
    }

    @Test
    void soyStateContainsWheatNotGlutenFree() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Soy Sauce").orElseThrow();
        assertFalse(p.dietaryTags().contains(DietaryTag.GLUTEN_FREE));
    }

    @Test
    void kbHasSubstantialCoverage() {
        assertTrue(IngredientKnowledgeBase.size() >= 300);
    }

    // Health supplements that were misclassified in staging
    @Test
    void healthSupplementsAreBulk() {
        assertEquals(GroceryCategory.BULK,
                IngredientKnowledgeBase.lookup("Baobab Boost Powder").orElseThrow().grocery());
        assertEquals(GroceryCategory.BULK,
                IngredientKnowledgeBase.lookup("Integral Collagen").orElseThrow().grocery());
        assertEquals(GroceryCategory.BULK,
                IngredientKnowledgeBase.lookup("Pure Stevia Extract Powder").orElseThrow().grocery());
        assertEquals(GroceryCategory.BULK,
                IngredientKnowledgeBase.lookup("Sunflower Lecithin").orElseThrow().grocery());
    }

    @Test
    void greenTeaBagsIsHousehold() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Green Tea Bags").orElseThrow();
        assertEquals(GroceryCategory.HOUSEHOLD, p.grocery());
        assertEquals(StorageCategory.PANTRY, p.storage());
    }

    @Test
    void matchaPowderIsHousehold() {
        IngredientProfile p = IngredientKnowledgeBase.lookup("Matcha Powder").orElseThrow();
        assertEquals(GroceryCategory.HOUSEHOLD, p.grocery());
    }
}
