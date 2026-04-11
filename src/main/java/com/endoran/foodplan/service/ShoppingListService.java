package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ShoppingAisle;
import com.endoran.foodplan.dto.ShoppingItem;
import com.endoran.foodplan.dto.ShoppingListResponse;
import com.endoran.foodplan.model.*;
import com.endoran.foodplan.model.PinnedRecipe;
import com.endoran.foodplan.model.SharedRecipeIngredient;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.PinnedRecipeRepository;
import com.endoran.foodplan.repository.InventoryItemRepository;
import com.endoran.foodplan.repository.MealPlanEntryRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShoppingListService {

    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PinnedRecipeRepository pinnedRecipeRepository;
    private final IngredientLinkerService ingredientLinkerService;

    public ShoppingListService(MealPlanEntryRepository mealPlanEntryRepository,
                                RecipeRepository recipeRepository,
                                IngredientRepository ingredientRepository,
                                InventoryItemRepository inventoryItemRepository,
                                PinnedRecipeRepository pinnedRecipeRepository,
                                IngredientLinkerService ingredientLinkerService) {
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.pinnedRecipeRepository = pinnedRecipeRepository;
        this.ingredientLinkerService = ingredientLinkerService;
    }

    public ShoppingListResponse generate(String orgId, LocalDate from, LocalDate to) {
        // 1. Load all meal plan entries in range
        List<MealPlanEntry> entries = mealPlanEntryRepository.findByOrgIdAndDateRange(orgId, from, to);

        // 2. Aggregate demand: ingredientId → (UnitFamily/unit → base quantity)
        // Key: ingredientId + unitFamily (or ingredientId + unit for NONE family)
        Map<String, DemandAccumulator> demand = new LinkedHashMap<>();

        for (MealPlanEntry entry : entries) {
            Recipe recipe = recipeRepository.findById(entry.getRecipeId()).orElse(null);

            // Fallback: resolve pinned recipe if local recipe not found
            if (recipe == null && entry.getPinnedId() != null) {
                PinnedRecipe pin = pinnedRecipeRepository.findById(entry.getPinnedId()).orElse(null);
                if (pin != null) {
                    accumulatePinnedDemand(demand, pin, entry.getServings());
                }
                continue;
            }
            if (recipe == null) continue;

            BigDecimal scaleFactor = BigDecimal.valueOf(entry.getServings())
                    .divide(BigDecimal.valueOf(recipe.getBaseServings()), 10, RoundingMode.HALF_UP);

            for (RecipeIngredient ri : recipe.getIngredients()) {
                BigDecimal scaledQty = ri.getMeasurement().getQuantity()
                        .multiply(scaleFactor);

                MeasurementUnit unit = ri.getMeasurement().getUnit();
                UnitConverter.UnitFamily family = UnitConverter.getFamily(unit);
                // Fall back to ingredientName when ingredientId is null (unlinked recipes)
                String idPart = ri.getIngredientId() != null
                        ? ri.getIngredientId()
                        : "name:" + ri.getIngredientName();
                String key = family == UnitConverter.UnitFamily.NONE
                        ? idPart + ":" + unit.name()
                        : idPart + ":" + family.name();

                demand.computeIfAbsent(key, k -> new DemandAccumulator(
                        ri.getIngredientId(), ri.getIngredientName(), family, unit
                )).add(scaledQty, unit);
            }
        }

        // 3. Subtract inventory
        List<InventoryItem> inventory = inventoryItemRepository.findByOrgId(orgId);
        Map<String, List<InventoryItem>> inventoryByIngredient = inventory.stream()
                .collect(Collectors.groupingBy(InventoryItem::getIngredientId));

        // 4. Build shopping items
        List<ShoppingItemWithCategory> items = new ArrayList<>();

        // Load all referenced ingredients for groceryCategory
        Set<String> ingredientIds = demand.values().stream()
                .map(d -> d.ingredientId)
                .collect(Collectors.toSet());
        Map<String, Ingredient> ingredientMap = new HashMap<>();
        ingredientRepository.findAllById(ingredientIds).forEach(i -> ingredientMap.put(i.getId(), i));

        for (DemandAccumulator acc : demand.values()) {
            Ingredient ingredient = ingredientMap.get(acc.ingredientId);

            // Skip ingredients excluded from shopping list (e.g., water)
            if (ingredient != null && ingredient.isShoppingListExclude()) continue;

            BigDecimal needed = acc.getTotal();

            // Subtract matching inventory (skip if ingredient is unlinked)
            List<InventoryItem> invItems = acc.ingredientId != null
                    ? inventoryByIngredient.getOrDefault(acc.ingredientId, List.of())
                    : List.of();
            for (InventoryItem inv : invItems) {
                UnitConverter.UnitFamily invFamily = UnitConverter.getFamily(inv.getUnit());
                if (acc.family == UnitConverter.UnitFamily.NONE) {
                    if (inv.getUnit() == acc.originalUnit) {
                        needed = needed.subtract(inv.getQuantity());
                    }
                } else if (invFamily == acc.family) {
                    needed = needed.subtract(UnitConverter.toBaseUnits(inv.getQuantity(), inv.getUnit()));
                }
            }

            needed = needed.max(BigDecimal.ZERO);
            if (needed.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Convert to readable unit
            MeasurementUnit displayUnit;
            BigDecimal displayQty;
            if (acc.family == UnitConverter.UnitFamily.NONE) {
                displayUnit = acc.originalUnit;
                displayQty = needed.setScale(2, RoundingMode.HALF_UP);
            } else {
                Measurement readable = UnitConverter.toReadableUnit(needed, acc.family);
                displayUnit = readable.getUnit();
                displayQty = readable.getQuantity();
            }

            GroceryCategory category = ingredient != null ? ingredient.getGroceryCategory() : GroceryCategory.HOUSEHOLD;

            items.add(new ShoppingItemWithCategory(
                    new ShoppingItem(acc.ingredientId, acc.ingredientName, displayQty, displayUnit),
                    category
            ));
        }

        // 5. Group by aisle
        Map<GroceryCategory, List<ShoppingItem>> byAisle = items.stream()
                .collect(Collectors.groupingBy(
                        ShoppingItemWithCategory::category,
                        LinkedHashMap::new,
                        Collectors.mapping(ShoppingItemWithCategory::item, Collectors.toList())
                ));

        List<ShoppingAisle> aisles = byAisle.entrySet().stream()
                .map(e -> new ShoppingAisle(e.getKey(), e.getValue()))
                .toList();

        return new ShoppingListResponse(aisles);
    }

    private void accumulatePinnedDemand(Map<String, DemandAccumulator> demand,
                                         PinnedRecipe pin, int servings) {
        // Lazy backfill: link ingredients if any are missing ingredientId
        if (ingredientLinkerService.needsLinking(pin.getIngredients())) {
            ingredientLinkerService.linkSharedIngredients(pin.getOrgId(), pin.getIngredients());
            pinnedRecipeRepository.save(pin);
        }

        BigDecimal scaleFactor = BigDecimal.valueOf(servings)
                .divide(BigDecimal.valueOf(pin.getBaseServings()), 10, RoundingMode.HALF_UP);

        for (SharedRecipeIngredient si : pin.getIngredients()) {
            MeasurementUnit unit;
            try {
                unit = MeasurementUnit.valueOf(si.getUnit());
            } catch (IllegalArgumentException e) {
                continue; // skip unknown units
            }
            BigDecimal scaledQty = BigDecimal.valueOf(si.getQuantity()).multiply(scaleFactor);
            UnitConverter.UnitFamily family = UnitConverter.getFamily(unit);

            // Use ingredientId when available (enables consolidation with local recipes)
            String idPart = si.getIngredientId() != null
                    ? si.getIngredientId()
                    : "name:" + si.getIngredientName();
            String key = family == UnitConverter.UnitFamily.NONE
                    ? idPart + ":" + unit.name()
                    : idPart + ":" + family.name();

            demand.computeIfAbsent(key, k -> new DemandAccumulator(
                    si.getIngredientId(), si.getIngredientName(), family, unit
            )).add(scaledQty, unit);
        }
    }

    private record ShoppingItemWithCategory(ShoppingItem item, GroceryCategory category) {}

    private static class DemandAccumulator {
        final String ingredientId;
        final String ingredientName;
        final UnitConverter.UnitFamily family;
        final MeasurementUnit originalUnit; // for NONE family
        private BigDecimal totalBase = BigDecimal.ZERO;

        DemandAccumulator(String ingredientId, String ingredientName,
                          UnitConverter.UnitFamily family, MeasurementUnit originalUnit) {
            this.ingredientId = ingredientId;
            this.ingredientName = ingredientName;
            this.family = family;
            this.originalUnit = originalUnit;
        }

        void add(BigDecimal qty, MeasurementUnit unit) {
            if (family == UnitConverter.UnitFamily.NONE) {
                totalBase = totalBase.add(qty);
            } else {
                totalBase = totalBase.add(UnitConverter.toBaseUnits(qty, unit));
            }
        }

        BigDecimal getTotal() {
            return totalBase;
        }
    }
}
