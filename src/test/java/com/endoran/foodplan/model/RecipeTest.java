package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {

    @Test
    void newRecipeHasEmptyIngredientList() {
        Recipe r = new Recipe();
        assertNotNull(r.getIngredients());
        assertTrue(r.getIngredients().isEmpty());
    }

    @Test
    void settersAndGetters() {
        Recipe r = new Recipe();
        r.setId("rec1");
        r.setName("Tacos");
        r.setInstructions("Cook the meat, assemble the tacos.");

        assertEquals("rec1", r.getId());
        assertEquals("Tacos", r.getName());
        assertEquals("Cook the meat, assemble the tacos.", r.getInstructions());
    }

    @Test
    void orgIdSetterAndGetter() {
        Recipe r = new Recipe();
        assertNull(r.getOrgId());
        r.setOrgId("org1");
        assertEquals("org1", r.getOrgId());
    }

    @Test
    void canAddIngredients() {
        Recipe r = new Recipe();

        RecipeIngredient cheese = new RecipeIngredient(
                "ing1", "Cheddar",
                new Measurement(new BigDecimal("2"), MeasurementUnit.CUP));

        RecipeIngredient lettuce = new RecipeIngredient(
                "ing2", "Lettuce",
                new Measurement(new BigDecimal("1"), MeasurementUnit.UNIT));

        r.setIngredients(List.of(cheese, lettuce));

        assertEquals(2, r.getIngredients().size());
        assertEquals("Cheddar", r.getIngredients().get(0).getIngredientName());
        assertEquals(MeasurementUnit.CUP, r.getIngredients().get(0).getMeasurement().getUnit());
    }
}
