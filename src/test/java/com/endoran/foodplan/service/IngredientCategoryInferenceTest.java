package com.endoran.foodplan.service;

import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.service.IngredientCategoryInference.InferredCategories;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngredientCategoryInferenceTest {

    @Test
    void chickenBreastIsMeat() {
        InferredCategories result = IngredientCategoryInference.infer("Chicken Breast");
        assertEquals(StorageCategory.REFRIGERATED, result.storage());
        assertEquals(GroceryCategory.MEAT, result.grocery());
    }

    @Test
    void frozenPeasIsFrozen() {
        InferredCategories result = IngredientCategoryInference.infer("Frozen Peas");
        assertEquals(StorageCategory.FROZEN, result.storage());
        assertEquals(GroceryCategory.FROZEN, result.grocery());
    }

    @Test
    void cheddarCheeseIsDairy() {
        InferredCategories result = IngredientCategoryInference.infer("Cheddar Cheese");
        assertEquals(StorageCategory.REFRIGERATED, result.storage());
        assertEquals(GroceryCategory.DAIRY, result.grocery());
    }

    @Test
    void cuminIsSpice() {
        InferredCategories result = IngredientCategoryInference.infer("Cumin");
        assertEquals(StorageCategory.SPICE_RACK, result.storage());
        assertEquals(GroceryCategory.SPICES, result.grocery());
    }

    @Test
    void garlicPowderIsSpiceNotCounter() {
        InferredCategories result = IngredientCategoryInference.infer("Garlic Powder");
        assertEquals(StorageCategory.SPICE_RACK, result.storage());
        assertEquals(GroceryCategory.SPICES, result.grocery());
    }

    @Test
    void garlicIsCounter() {
        InferredCategories result = IngredientCategoryInference.infer("Garlic");
        assertEquals(StorageCategory.COUNTER, result.storage());
        assertEquals(GroceryCategory.PRODUCE, result.grocery());
    }

    @Test
    void oliveOilIsOilsCondiments() {
        InferredCategories result = IngredientCategoryInference.infer("Olive Oil");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.OILS_CONDIMENTS, result.grocery());
    }

    @Test
    void cilantroIsFreshProduce() {
        InferredCategories result = IngredientCategoryInference.infer("Cilantro");
        assertEquals(StorageCategory.FRESH, result.storage());
        assertEquals(GroceryCategory.PRODUCE, result.grocery());
    }

    @Test
    void tomatoPasteIsCanned() {
        InferredCategories result = IngredientCategoryInference.infer("Tomato Paste");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.CANNED, result.grocery());
    }

    @Test
    void coconutMilkIsCannedNotDairy() {
        InferredCategories result = IngredientCategoryInference.infer("Coconut Milk");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.CANNED, result.grocery());
    }

    @Test
    void butterIsDairy() {
        InferredCategories result = IngredientCategoryInference.infer("Butter");
        assertEquals(StorageCategory.REFRIGERATED, result.storage());
        assertEquals(GroceryCategory.DAIRY, result.grocery());
    }

    @Test
    void flourIsBaking() {
        InferredCategories result = IngredientCategoryInference.infer("All-Purpose Flour");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.BAKING, result.grocery());
    }

    @Test
    void onionIsCounter() {
        InferredCategories result = IngredientCategoryInference.infer("Yellow Onions");
        assertEquals(StorageCategory.COUNTER, result.storage());
        assertEquals(GroceryCategory.PRODUCE, result.grocery());
    }

    @Test
    void lettuceDefaultsToProduce() {
        InferredCategories result = IngredientCategoryInference.infer("Romaine Lettuce");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.PRODUCE, result.grocery());
    }

    @Test
    void caseInsensitive() {
        InferredCategories result = IngredientCategoryInference.infer("CHICKEN breast");
        assertEquals(StorageCategory.REFRIGERATED, result.storage());
        assertEquals(GroceryCategory.MEAT, result.grocery());
    }

    @Test
    void garamMasalaIsSpice() {
        InferredCategories result = IngredientCategoryInference.infer("Garam Masala");
        assertEquals(StorageCategory.SPICE_RACK, result.storage());
        assertEquals(GroceryCategory.SPICES, result.grocery());
    }

    @Test
    void honeyIsOilsCondiments() {
        InferredCategories result = IngredientCategoryInference.infer("Honey");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.OILS_CONDIMENTS, result.grocery());
    }

    @Test
    void chickenStockIsCanned() {
        InferredCategories result = IngredientCategoryInference.infer("Chicken Stock");
        assertEquals(StorageCategory.PANTRY, result.storage());
        assertEquals(GroceryCategory.CANNED, result.grocery());
    }
}
