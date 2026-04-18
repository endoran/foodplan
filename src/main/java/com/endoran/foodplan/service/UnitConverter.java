package com.endoran.foodplan.service;

import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.MeasurementUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class UnitConverter {

    public enum UnitFamily { VOLUME, WEIGHT, NONE }

    // Conversion factors to base unit (TSP for volume, OZ for weight)
    private static final Map<MeasurementUnit, BigDecimal> TO_BASE = new EnumMap<>(MeasurementUnit.class);

    static {
        // Volume → TSP
        TO_BASE.put(MeasurementUnit.TSP, BigDecimal.ONE);
        TO_BASE.put(MeasurementUnit.TBSP, new BigDecimal("3"));
        TO_BASE.put(MeasurementUnit.FL_OZ, new BigDecimal("6"));
        TO_BASE.put(MeasurementUnit.CUP, new BigDecimal("48"));
        TO_BASE.put(MeasurementUnit.PINT, new BigDecimal("96"));
        TO_BASE.put(MeasurementUnit.QUART, new BigDecimal("192"));
        TO_BASE.put(MeasurementUnit.HALF_GALLON, new BigDecimal("384"));
        TO_BASE.put(MeasurementUnit.GALLON, new BigDecimal("768"));
        // Metric volume → TSP (1 tsp ≈ 4.929 ml)
        TO_BASE.put(MeasurementUnit.ML, new BigDecimal("0.2029"));
        TO_BASE.put(MeasurementUnit.L, new BigDecimal("202.884"));
        // Weight → OZ
        TO_BASE.put(MeasurementUnit.OZ, BigDecimal.ONE);
        TO_BASE.put(MeasurementUnit.LBS, new BigDecimal("16"));
        // Metric weight → OZ (1 oz ≈ 28.3495 g)
        TO_BASE.put(MeasurementUnit.G, new BigDecimal("0.03527"));
        TO_BASE.put(MeasurementUnit.KG, new BigDecimal("35.274"));
    }

    private static final Map<MeasurementUnit, UnitFamily> FAMILIES = new EnumMap<>(MeasurementUnit.class);

    static {
        FAMILIES.put(MeasurementUnit.TSP, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.TBSP, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.FL_OZ, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.CUP, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.PINT, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.QUART, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.HALF_GALLON, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.GALLON, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.ML, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.L, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.OZ, UnitFamily.WEIGHT);
        FAMILIES.put(MeasurementUnit.LBS, UnitFamily.WEIGHT);
        FAMILIES.put(MeasurementUnit.G, UnitFamily.WEIGHT);
        FAMILIES.put(MeasurementUnit.KG, UnitFamily.WEIGHT);
    }

    // Ordered largest → smallest for readable output (grocery-friendly units only)
    private static final List<MeasurementUnit> VOLUME_UNITS = List.of(
            MeasurementUnit.CUP, MeasurementUnit.FL_OZ, MeasurementUnit.TBSP, MeasurementUnit.TSP);
    private static final List<MeasurementUnit> WEIGHT_UNITS = List.of(
            MeasurementUnit.LBS, MeasurementUnit.OZ);

    private UnitConverter() {}

    public static UnitFamily getFamily(MeasurementUnit unit) {
        return FAMILIES.getOrDefault(unit, UnitFamily.NONE);
    }

    public static BigDecimal toBaseUnits(BigDecimal qty, MeasurementUnit unit) {
        BigDecimal factor = TO_BASE.get(unit);
        if (factor == null) {
            return qty;
        }
        return qty.multiply(factor);
    }

    public static Measurement toReadableUnit(BigDecimal baseQty, UnitFamily family) {
        List<MeasurementUnit> units = family == UnitFamily.VOLUME ? VOLUME_UNITS : WEIGHT_UNITS;
        for (MeasurementUnit unit : units) {
            BigDecimal factor = TO_BASE.get(unit);
            BigDecimal converted = baseQty.divide(factor, 2, RoundingMode.HALF_UP);
            if (converted.compareTo(BigDecimal.ONE) >= 0) {
                return new Measurement(converted, unit);
            }
        }
        // Fallback to smallest unit
        MeasurementUnit smallest = units.get(units.size() - 1);
        return new Measurement(baseQty.setScale(2, RoundingMode.HALF_UP), smallest);
    }
}
