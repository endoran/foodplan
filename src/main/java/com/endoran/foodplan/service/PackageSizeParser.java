package com.endoran.foodplan.service;

import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.MeasurementUnit;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses store package size strings like "16 oz", "1 LB", "32 fl oz" into Measurement objects.
 */
public final class PackageSizeParser {

    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "(\\d+\\.?\\d*)\\s*(fl\\s*oz|oz|lbs?|gal(?:lon)?|qt|pt|cup|tbsp|tsp)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, MeasurementUnit> UNIT_MAP = Map.ofEntries(
            Map.entry("oz", MeasurementUnit.OZ),
            Map.entry("fl oz", MeasurementUnit.OZ),
            Map.entry("floz", MeasurementUnit.OZ),
            Map.entry("lb", MeasurementUnit.LBS),
            Map.entry("lbs", MeasurementUnit.LBS),
            Map.entry("gal", MeasurementUnit.GALLON),
            Map.entry("gallon", MeasurementUnit.GALLON),
            Map.entry("qt", MeasurementUnit.QUART),
            Map.entry("pt", MeasurementUnit.PINT),
            Map.entry("cup", MeasurementUnit.CUP),
            Map.entry("tbsp", MeasurementUnit.TBSP),
            Map.entry("tsp", MeasurementUnit.TSP)
    );

    private PackageSizeParser() {}

    /**
     * Parse a package size string into a Measurement.
     * Returns null if the string cannot be parsed or contains no recognized measurement unit.
     */
    public static Measurement parse(String sizeString) {
        if (sizeString == null || sizeString.isBlank()) return null;

        // For strings like "4 sticks / 16 oz", take the last measurement segment
        String[] parts = sizeString.split("/");
        String toParse = parts[parts.length - 1].trim();

        Matcher matcher = SIZE_PATTERN.matcher(toParse);
        if (!matcher.find()) return null;

        try {
            BigDecimal qty = new BigDecimal(matcher.group(1));
            String unitStr = matcher.group(2).toLowerCase().replaceAll("\\s+", "");
            MeasurementUnit unit = UNIT_MAP.get(unitStr);
            if (unit == null) return null;
            return new Measurement(qty, unit);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
