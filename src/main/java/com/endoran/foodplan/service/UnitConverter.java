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
        TO_BASE.put(MeasurementUnit.CUP, new BigDecimal("48"));
        TO_BASE.put(MeasurementUnit.PINT, new BigDecimal("96"));
        TO_BASE.put(MeasurementUnit.QUART, new BigDecimal("192"));
        TO_BASE.put(MeasurementUnit.HALF_GALLON, new BigDecimal("384"));
        TO_BASE.put(MeasurementUnit.GALLON, new BigDecimal("768"));
        // Weight → OZ
        TO_BASE.put(MeasurementUnit.OZ, BigDecimal.ONE);
        TO_BASE.put(MeasurementUnit.LBS, new BigDecimal("16"));
    }

    private static final Map<MeasurementUnit, UnitFamily> FAMILIES = new EnumMap<>(MeasurementUnit.class);

    static {
        FAMILIES.put(MeasurementUnit.TSP, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.TBSP, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.CUP, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.PINT, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.QUART, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.HALF_GALLON, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.GALLON, UnitFamily.VOLUME);
        FAMILIES.put(MeasurementUnit.OZ, UnitFamily.WEIGHT);
        FAMILIES.put(MeasurementUnit.LBS, UnitFamily.WEIGHT);
    }

    // Ordered largest → smallest for readable output (grocery-friendly units only)
    private static final List<MeasurementUnit> VOLUME_UNITS = List.of(
            MeasurementUnit.CUP, MeasurementUnit.TBSP, MeasurementUnit.TSP);
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
