package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ShoppingAisle;
import com.endoran.foodplan.dto.ShoppingItem;
import com.endoran.foodplan.dto.ShoppingListResponse;
import com.endoran.foodplan.model.*;
import com.endoran.foodplan.repository.IngredientRepository;
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

    public ShoppingListService(MealPlanEntryRepository mealPlanEntryRepository,
                                RecipeRepository recipeRepository,
                                IngredientRepository ingredientRepository,
                                InventoryItemRepository inventoryItemRepository) {
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public ShoppingListResponse generate(String orgId, LocalDate from, LocalDate to) {
        // 1. Load all meal plan entries in range
        List<MealPlanEntry> entries = mealPlanEntryRepository.findByOrgIdAndDateRange(orgId, from, to);

        // 2. Aggregate demand: ingredientId → (UnitFamily/unit → base quantity)
        // Key: ingredientId + unitFamily (or ingredientId + unit for NONE family)
        Map<String, DemandAccumulator> demand = new LinkedHashMap<>();

        for (MealPlanEntry entry : entries) {
            Recipe recipe = recipeRepository.findById(entry.getRecipeId()).orElse(null);
            if (recipe == null) continue;

            BigDecimal scaleFactor = BigDecimal.valueOf(entry.getServings())
                    .divide(BigDecimal.valueOf(recipe.getBaseServings()), 10, RoundingMode.HALF_UP);

            for (RecipeIngredient ri : recipe.getIngredients()) {
                BigDecimal scaledQty = ri.getMeasurement().getQuantity()
                        .multiply(scaleFactor);

                MeasurementUnit unit = ri.getMeasurement().getUnit();
                UnitConverter.UnitFamily family = UnitConverter.getFamily(unit);
                String key = family == UnitConverter.UnitFamily.NONE
                        ? ri.getIngredientId() + ":" + unit.name()
                        : ri.getIngredientId() + ":" + family.name();

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

            // Subtract matching inventory
            List<InventoryItem> invItems = inventoryByIngredient.getOrDefault(acc.ingredientId, List.of());
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
