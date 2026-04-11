package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MeasurementTest {

    @Test
    void defaultConstructorCreatesEmptyMeasurement() {
        Measurement m = new Measurement();
        assertNull(m.getQuantity());
        assertNull(m.getUnit());
    }

    @Test
    void parameterizedConstructorSetsFields() {
        Measurement m = new Measurement(new BigDecimal("2.5"), MeasurementUnit.CUP);
        assertEquals(new BigDecimal("2.5"), m.getQuantity());
        assertEquals(MeasurementUnit.CUP, m.getUnit());
    }

    @Test
    void settersUpdateFields() {
        Measurement m = new Measurement();
        m.setQuantity(new BigDecimal("1"));
        m.setUnit(MeasurementUnit.TSP);
        assertEquals(new BigDecimal("1"), m.getQuantity());
        assertEquals(MeasurementUnit.TSP, m.getUnit());
    }

    @Test
    void equalMeasurementsAreEqual() {
        Measurement a = new Measurement(new BigDecimal("3"), MeasurementUnit.TBSP);
        Measurement b = new Measurement(new BigDecimal("3"), MeasurementUnit.TBSP);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentQuantitiesAreNotEqual() {
        Measurement a = new Measurement(new BigDecimal("1"), MeasurementUnit.CUP);
        Measurement b = new Measurement(new BigDecimal("2"), MeasurementUnit.CUP);
        assertNotEquals(a, b);
    }

    @Test
    void differentUnitsAreNotEqual() {
        Measurement a = new Measurement(new BigDecimal("1"), MeasurementUnit.CUP);
        Measurement b = new Measurement(new BigDecimal("1"), MeasurementUnit.PINT);
        assertNotEquals(a, b);
    }

    @Test
    void equalsHandlesNullAndOtherTypes() {
        Measurement m = new Measurement(BigDecimal.ONE, MeasurementUnit.OZ);
        assertNotEquals(null, m);
        assertNotEquals("not a measurement", m);
    }
}
