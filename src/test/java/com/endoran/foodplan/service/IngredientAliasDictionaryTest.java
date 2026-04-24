package com.endoran.foodplan.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IngredientAliasDictionaryTest {

    @Test
    void parmesanAliasResolvesToCheese() {
        assertEquals("Parmesan Cheese", IngredientAliasDictionary.resolve("parmesan"));
    }

    @Test
    void parmiggianoResolves() {
        assertEquals("Parmesan Cheese", IngredientAliasDictionary.resolve("parmigiano-reggiano"));
    }

    @Test
    void scallionResolvesToGreenOnion() {
        assertEquals("Green Onion", IngredientAliasDictionary.resolve("scallion"));
    }

    @Test
    void evooResolvesToOliveOil() {
        assertEquals("Olive Oil", IngredientAliasDictionary.resolve("evoo"));
    }

    @Test
    void unknownNamePassesThrough() {
        assertEquals("Unicorn Tears", IngredientAliasDictionary.resolve("Unicorn Tears"));
    }

    @Test
    void caseInsensitiveLookup() {
        assertEquals("Ground Beef", IngredientAliasDictionary.resolve("HAMBURGER MEAT"));
    }

    @Test
    void kosherSaltStaysDistinct() {
        assertEquals("Kosher Salt", IngredientAliasDictionary.resolve("Kosher Salt"));
    }

    @Test
    void seaSaltStaysDistinct() {
        assertEquals("Sea Salt", IngredientAliasDictionary.resolve("Sea Salt"));
    }

    @Test
    void resolveAndNormalizeFullPipeline() {
        assertEquals("Green Onion", IngredientAliasDictionary.resolveAndNormalize("  SCALLIONS  "));
    }

    @Test
    void resolveAndNormalizeUnknown() {
        assertEquals("Pink Himalayan Salt", IngredientAliasDictionary.resolveAndNormalize("pink himalayan salt"));
    }

    @Test
    void nullReturnsNull() {
        assertNull(IngredientAliasDictionary.resolve(null));
    }

    @Test
    void blankPassesThrough() {
        assertEquals("  ", IngredientAliasDictionary.resolve("  "));
    }

    @Test
    void heavyCreamAlias() {
        assertEquals("Heavy Cream", IngredientAliasDictionary.resolve("heavy whipping cream"));
    }

    @Test
    void allPurposeFlourVariants() {
        assertEquals("All-Purpose Flour", IngredientAliasDictionary.resolve("ap flour"));
        assertEquals("All-Purpose Flour", IngredientAliasDictionary.resolve("plain flour"));
        assertEquals("All-Purpose Flour", IngredientAliasDictionary.resolve("flour"));
    }

    @Test
    void groundBeefVariants() {
        assertEquals("Ground Beef", IngredientAliasDictionary.resolve("hamburger meat"));
        assertEquals("Ground Beef", IngredientAliasDictionary.resolve("ground chuck"));
        assertEquals("Ground Beef", IngredientAliasDictionary.resolve("beef mince"));
    }

    @Test
    void chickpeasGarbanzoBeans() {
        assertEquals("Chickpeas", IngredientAliasDictionary.resolve("garbanzo beans"));
    }
}
