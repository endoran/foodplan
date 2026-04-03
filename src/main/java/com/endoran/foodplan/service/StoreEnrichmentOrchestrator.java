package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ShoppingAisle;
import com.endoran.foodplan.dto.ShoppingItem;
import com.endoran.foodplan.dto.ShoppingListResponse;
import com.endoran.foodplan.dto.StoreProductMatch;
import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.MeasurementUnit;
import com.endoran.foodplan.model.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                StoreType.FRED_MEYER, kroger
        );
    }

    public ShoppingListResponse enrich(ShoppingListResponse baseList, StoreType storeType) {
        StoreEnrichmentService service = services.get(storeType);
        if (service == null) return baseList;

        if (storeType == StoreType.FRED_MEYER && service instanceof KrogerEnrichmentService k && !k.isConfigured()) {
            return baseList;
        }

        try {
            List<String> names = baseList.aisles().stream()
                    .flatMap(a -> a.items().stream())
                    .map(ShoppingItem::ingredientName)
                    .toList();

            Map<String, StoreProductMatch> matches = service.enrich(names);

            List<ShoppingAisle> enrichedAisles = baseList.aisles().stream()
                    .map(aisle -> new ShoppingAisle(
                            aisle.category(),
                            aisle.items().stream()
                                    .map(item -> applyMatch(item, matches.get(item.ingredientName())))
                                    .toList()
                    ))
                    .toList();

            return new ShoppingListResponse(enrichedAisles, service.storeName());
        } catch (Exception e) {
            log.warn("Store enrichment failed for {}: {}", storeType, e.getMessage());
            return baseList;
        }
    }

    private ShoppingItem applyMatch(ShoppingItem item, StoreProductMatch match) {
        if (match == null) return item;

        Integer qtyNeeded = null;
        BigDecimal totalPrice = match.storePrice();
        BigDecimal totalPromo = match.storePromoPrice();

        Measurement packageMeasurement = PackageSizeParser.parse(match.storePackageSize());
        if (packageMeasurement != null) {
            MeasurementUnit pkgUnit = packageMeasurement.getUnit();
            MeasurementUnit shopUnit = item.unit();
            UnitConverter.UnitFamily packageFamily = UnitConverter.getFamily(pkgUnit);
            UnitConverter.UnitFamily shoppingFamily = UnitConverter.getFamily(shopUnit);

            // Cross-family bridge: recipe OZ (weight) ↔ store FL_OZ (volume).
            // 1 oz weight ≈ 1 fl oz volume for dense items — close enough for shopping.
            if (shoppingFamily == UnitConverter.UnitFamily.WEIGHT
                    && shopUnit == MeasurementUnit.OZ
                    && packageFamily == UnitConverter.UnitFamily.VOLUME) {
                shopUnit = MeasurementUnit.FL_OZ;
                shoppingFamily = UnitConverter.UnitFamily.VOLUME;
            }

            if (packageFamily != UnitConverter.UnitFamily.NONE
                    && packageFamily == shoppingFamily) {
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

        return new ShoppingItem(
                item.ingredientId(),
                item.ingredientName(),
                item.quantity(),
                item.unit(),
                match.storeAisle(),
                totalPrice,
                totalPromo,
                match.storeStockLevel(),
                match.storeProductName(),
                match.storePackageSize(),
                qtyNeeded
        );
    }
}
