package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RecipeIngredientTest {

    @Test
    void defaultConstructor() {
        RecipeIngredient ri = new RecipeIngredient();
        assertNull(ri.getIngredientId());
        assertNull(ri.getIngredientName());
        assertNull(ri.getMeasurement());
    }

    @Test
    void parameterizedConstructor() {
        Measurement m = new Measurement(new BigDecimal("0.5"), MeasurementUnit.TSP);
        RecipeIngredient ri = new RecipeIngredient("id1", "Salt", m);

        assertEquals("id1", ri.getIngredientId());
        assertEquals("Salt", ri.getIngredientName());
        assertSame(m, ri.getMeasurement());
    }

    @Test
    void setters() {
        RecipeIngredient ri = new RecipeIngredient();
        Measurement m = new Measurement(new BigDecimal("3"), MeasurementUnit.LBS);

        ri.setIngredientId("x");
        ri.setIngredientName("Ground Beef");
        ri.setMeasurement(m);

        assertEquals("x", ri.getIngredientId());
        assertEquals("Ground Beef", ri.getIngredientName());
        assertEquals(new BigDecimal("3"), ri.getMeasurement().getQuantity());
    }
}
