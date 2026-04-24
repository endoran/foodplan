package com.endoran.foodplan.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IngredientNameNormalizerTest {

    @Test
    void basicTitleCase() {
        assertEquals("Kosher Salt", IngredientNameNormalizer.normalize("kosher salt"));
    }

    @Test
    void allCapsToTitleCase() {
        assertEquals("Kosher Salt", IngredientNameNormalizer.normalize("KOSHER SALT"));
    }

    @Test
    void mixedCaseNormalized() {
        assertEquals("Red Bell Pepper", IngredientNameNormalizer.normalize("rEd BELL pepper"));
    }

    @Test
    void lowercaseArticles() {
        assertEquals("Salt and Pepper", IngredientNameNormalizer.normalize("salt and pepper"));
    }

    @Test
    void lowercasePrepositions() {
        assertEquals("Cream of Mushroom Soup", IngredientNameNormalizer.normalize("cream of mushroom soup"));
    }

    @Test
    void firstWordAlwaysCapitalized() {
        assertEquals("A Pinch of Salt", IngredientNameNormalizer.normalize("a pinch of salt"));
    }

    @Test
    void collapsesWhitespace() {
        assertEquals("Garlic Powder", IngredientNameNormalizer.normalize("  garlic   powder  "));
    }

    @Test
    void stripsTrailingPunctuation() {
        assertEquals("Garlic Powder", IngredientNameNormalizer.normalize("garlic powder."));
        assertEquals("Garlic Powder", IngredientNameNormalizer.normalize("garlic powder,"));
        assertEquals("Garlic Powder", IngredientNameNormalizer.normalize("garlic powder;"));
    }

    @Test
    void parentheticalHandling() {
        assertEquals("Parmesan Cheese (Powder)", IngredientNameNormalizer.normalize("parmesan cheese (powder)"));
    }

    @Test
    void parentheticalFirstWordCapitalized() {
        assertEquals("Red Pepper (Diced)", IngredientNameNormalizer.normalize("red pepper (diced)"));
    }

    @Test
    void hyphenatedWordsPreserved() {
        assertEquals("All-purpose Flour", IngredientNameNormalizer.normalize("all-purpose flour"));
    }

    @Test
    void singleWord() {
        assertEquals("Cilantro", IngredientNameNormalizer.normalize("cilantro"));
    }

    @Test
    void nullReturnsNull() {
        assertNull(IngredientNameNormalizer.normalize(null));
    }

    @Test
    void blankReturnsBlank() {
        assertEquals("   ", IngredientNameNormalizer.normalize("   "));
    }

    @Test
    void emptyReturnsEmpty() {
        assertEquals("", IngredientNameNormalizer.normalize(""));
    }

    @Test
    void alreadyNormalized() {
        assertEquals("Chicken Breast", IngredientNameNormalizer.normalize("Chicken Breast"));
    }
}
