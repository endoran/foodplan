package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ShoppingAisle;
import com.endoran.foodplan.dto.ShoppingItem;
import com.endoran.foodplan.dto.ShoppingListResponse;
import com.endoran.foodplan.dto.StoreProductAlternative;
import com.endoran.foodplan.dto.StoreProductMatch;
import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.MeasurementUnit;
import com.endoran.foodplan.model.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StoreEnrichmentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(StoreEnrichmentOrchestrator.class);

    private final Map<StoreType, StoreEnrichmentService> services;

    public StoreEnrichmentOrchestrator(
            ChefStoreEnrichmentService chefStore,
            KrogerEnrichmentService kroger) {
        this.services = Map.of(
                StoreType.CHEF_STORE, chefStore,
                StoreType.FRED_MEYER, kroger,
                StoreType.FRED_MEYER_ONLINE, kroger
        );
    }

    public ShoppingListResponse enrich(ShoppingListResponse baseList, StoreType storeType) {
        StoreEnrichmentService service = services.get(storeType);
        if (service == null) return baseList;

        if ((storeType == StoreType.FRED_MEYER || storeType == StoreType.FRED_MEYER_ONLINE)
                && service instanceof KrogerEnrichmentService k && !k.isConfigured()) {
            return baseList;
        }

        boolean isOnlineOnly = storeType == StoreType.FRED_MEYER_ONLINE;
        boolean isInStore = storeType == StoreType.FRED_MEYER;

        try {
            List<String> names = baseList.aisles().stream()
                    .flatMap(a -> a.items().stream())
                    .map(ShoppingItem::ingredientName)
                    .toList();

            Map<String, List<StoreProductMatch>> allMatches = service.enrich(names);

            // Apply fulfillment filtering to each match list
            Map<String, List<StoreProductMatch>> filteredMatches;
            if (isInStore) {
                filteredMatches = filterMatchesInStore(allMatches);
            } else if (isOnlineOnly) {
                filteredMatches = filterMatchesOnlineOnly(allMatches);
            } else {
                filteredMatches = allMatches;
            }

            List<ShoppingAisle> enrichedAisles = baseList.aisles().stream()
                    .map(aisle -> new ShoppingAisle(
                            aisle.category(),
                            aisle.items().stream()
                                    .map(item -> applyMatches(item, filteredMatches.get(item.ingredientName())))
                                    .toList()
                    ))
                    .toList();

            String displayName = storeType == StoreType.FRED_MEYER_ONLINE
                    ? "Fred Meyer (Online)" : service.storeName();
            return new ShoppingListResponse(enrichedAisles, displayName);
        } catch (Exception e) {
            log.warn("Store enrichment failed for {}: {}", storeType, e.getMessage());
            return baseList;
        }
    }

    private Map<String, List<StoreProductMatch>> filterMatchesInStore(Map<String, List<StoreProductMatch>> matches) {
        Map<String, List<StoreProductMatch>> filtered = new HashMap<>();
        for (var entry : matches.entrySet()) {
            List<StoreProductMatch> list = entry.getValue().stream()
                    .map(m -> isFulfillmentAisle(m.storeAisle())
                            ? new StoreProductMatch(m.storeProductId(), null, m.storePrice(),
                                    m.storePromoPrice(), m.storeStockLevel(),
                                    m.storeProductName(), m.storePackageSize())
                            : m)
                    .toList();
            filtered.put(entry.getKey(), list);
        }
        return filtered;
    }

    private Map<String, List<StoreProductMatch>> filterMatchesOnlineOnly(Map<String, List<StoreProductMatch>> matches) {
        Map<String, List<StoreProductMatch>> filtered = new HashMap<>();
        for (var entry : matches.entrySet()) {
            List<StoreProductMatch> list = entry.getValue().stream()
                    .filter(m -> isFulfillmentAisle(m.storeAisle()))
                    .toList();
            if (!list.isEmpty()) {
                filtered.put(entry.getKey(), list);
            }
        }
        return filtered;
    }

    private boolean isFulfillmentAisle(String aisle) {
        if (aisle == null || aisle.isBlank()) return false;
        String digits = aisle.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return false;
        try {
            return Integer.parseInt(digits) >= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private ShoppingItem applyMatches(ShoppingItem item, List<StoreProductMatch> matches) {
        if (matches == null || matches.isEmpty()) return item;

        // Rank by match quality
        List<StoreProductMatch> ranked = ProductMatchScorer.rankMatches(item.ingredientName(), matches);

        // Build alternatives with pre-computed quantities and totals
        List<StoreProductAlternative> alternatives = new ArrayList<>();
        for (StoreProductMatch match : ranked) {
            alternatives.add(buildAlternative(item, match));
        }

        return new ShoppingItem(
                item.ingredientId(),
                item.ingredientName(),
                item.quantity(),
                item.unit(),
                alternatives
        );
    }

    private StoreProductAlternative buildAlternative(ShoppingItem item, StoreProductMatch match) {
        Integer qtyNeeded = null;
        BigDecimal totalPrice = match.storePrice();
        BigDecimal totalPromo = match.storePromoPrice();

        Measurement packageMeasurement = PackageSizeParser.parse(match.storePackageSize());
        if (packageMeasurement != null) {
            MeasurementUnit pkgUnit = packageMeasurement.getUnit();
            MeasurementUnit shopUnit = item.unit();
            UnitConverter.UnitFamily packageFamily = UnitConverter.getFamily(pkgUnit);
            UnitConverter.UnitFamily shoppingFamily = UnitConverter.getFamily(shopUnit);

            if (shoppingFamily == UnitConverter.UnitFamily.WEIGHT
                    && shopUnit == MeasurementUnit.OZ
                    && packageFamily == UnitConverter.UnitFamily.VOLUME) {
                shopUnit = MeasurementUnit.FL_OZ;
                shoppingFamily = UnitConverter.UnitFamily.VOLUME;
            }

            if (packageFamily == shoppingFamily
                    && (packageFamily != UnitConverter.UnitFamily.NONE || pkgUnit == shopUnit)) {
                BigDecimal packageBase = UnitConverter.toBaseUnits(
                        packageMeasurement.getQuantity(), pkgUnit);
                BigDecimal shoppingBase = UnitConverter.toBaseUnits(item.quantity(), shopUnit);

                if (packageBase.compareTo(BigDecimal.ZERO) > 0) {
                    int packages = shoppingBase.divide(packageBase, 0, RoundingMode.CEILING).intValue();
                    packages = Math.max(1, packages);
                    qtyNeeded = packages;

                    if (match.storePrice() != null) {
                        totalPrice = match.storePrice().multiply(BigDecimal.valueOf(packages));
                    }
                    if (match.storePromoPrice() != null) {
                        totalPromo = match.storePromoPrice().multiply(BigDecimal.valueOf(packages));
                    }
                }
            } else {
                log.debug("Cannot calculate packages for '{}': shopping unit {} ({}) vs package unit {} ({})",
                        item.ingredientName(), item.unit(), shoppingFamily, pkgUnit, packageFamily);
            }
        }

        return new StoreProductAlternative(
                match.storeProductId(),
                match.storeProductName(),
                match.storeAisle(),
                match.storePrice(),
                match.storePromoPrice(),
                match.storeStockLevel(),
                match.storePackageSize(),
                qtyNeeded,
                totalPrice,
                totalPromo
        );
    }
}
