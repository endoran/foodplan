package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IngredientTest {

    @Test
    void settersAndGetters() {
        Ingredient i = new Ingredient();
        i.setId("abc123");
        i.setName("Cheddar Cheese");
        i.setGroceryCategory(GroceryCategory.DAIRY);
        i.setStorageCategory(StorageCategory.REFRIGERATED);

        assertEquals("abc123", i.getId());
        assertEquals("Cheddar Cheese", i.getName());
        assertEquals(GroceryCategory.DAIRY, i.getGroceryCategory());
        assertEquals(StorageCategory.REFRIGERATED, i.getStorageCategory());
    }

    @Test
    void orgIdSetterAndGetter() {
        Ingredient i = new Ingredient();
        assertNull(i.getOrgId());
        i.setOrgId("org1");
        assertEquals("org1", i.getOrgId());
    }

    @Test
    void equalsSameIdAndName() {
        Ingredient a = new Ingredient();
        a.setId("1");
        a.setName("salt");

        Ingredient b = new Ingredient();
        b.setId("1");
        b.setName("salt");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentIdMeansNotEqual() {
        Ingredient a = new Ingredient();
        a.setId("1");
        a.setName("salt");

        Ingredient b = new Ingredient();
        b.setId("2");
        b.setName("salt");

        assertNotEquals(a, b);
    }

    @Test
    void dietaryTagsDefaultToEmptySet() {
        Ingredient i = new Ingredient();
        assertNotNull(i.getDietaryTags());
        assertTrue(i.getDietaryTags().isEmpty());
    }

    @Test
    void dietaryTagsSetterAndGetter() {
        Ingredient i = new Ingredient();
        i.setDietaryTags(Set.of(DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE));
        assertEquals(2, i.getDietaryTags().size());
        assertTrue(i.getDietaryTags().contains(DietaryTag.GLUTEN_FREE));
        assertTrue(i.getDietaryTags().contains(DietaryTag.DAIRY_FREE));
    }
}
