package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.CreateMealPlanEntryRequest;
import com.endoran.foodplan.dto.DietaryWarning;
import com.endoran.foodplan.dto.MealPlanEntryResponse;
import com.endoran.foodplan.dto.UpdateMealPlanEntryRequest;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.InventoryItem;
import com.endoran.foodplan.model.MealPlanEntry;
import com.endoran.foodplan.model.MealStatus;
import com.endoran.foodplan.model.MealType;
import com.endoran.foodplan.model.Recipe;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.InventoryItemRepository;
import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.PinnedRecipe;
import com.endoran.foodplan.model.SharedRecipeIngredient;
import com.endoran.foodplan.repository.MealPlanEntryRepository;
import com.endoran.foodplan.repository.PinnedRecipeRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import com.endoran.foodplan.model.MeasurementUnit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class MealPlanEntryService {

    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PinnedRecipeRepository pinnedRecipeRepository;
    private final IngredientLinkerService ingredientLinkerService;

    public MealPlanEntryService(MealPlanEntryRepository mealPlanEntryRepository,
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

    public MealPlanEntryResponse create(String orgId, CreateMealPlanEntryRequest request) {
        MealPlanEntry entry = new MealPlanEntry();
        entry.setOrgId(orgId);
        entry.setDate(request.date());
        entry.setMealType(request.mealType());
        entry.setServings(request.servings());
        entry.setNotes(request.notes());

        if (request.pinnedId() != null && !request.pinnedId().isBlank()) {
            PinnedRecipe pin = pinnedRecipeRepository.findByIdAndOrgId(request.pinnedId(), orgId)
                    .orElseThrow(() -> new RecipeNotFoundException("Pinned recipe " + request.pinnedId()));
            entry.setRecipeId(request.pinnedId());
            entry.setRecipeName(pin.getName());
            entry.setPinnedId(request.pinnedId());
        } else {
            Recipe recipe = findRecipeByIdAndOrg(orgId, request.recipeId());
            entry.setRecipeId(recipe.getId());
            entry.setRecipeName(recipe.getName());
        }

        entry = mealPlanEntryRepository.save(entry);
        return toResponse(entry);
    }

    public MealPlanEntryResponse getById(String orgId, String id) {
        MealPlanEntry entry = findByIdAndOrg(orgId, id);
        return toResponse(entry);
    }

    public List<MealPlanEntryResponse> list(String orgId, LocalDate from, LocalDate to, MealType mealType) {
        List<MealPlanEntry> entries;
        if (from != null && to != null && mealType != null) {
            entries = mealPlanEntryRepository.findByOrgIdAndDateRangeAndMealType(orgId, from, to, mealType);
        } else if (from != null && to != null) {
            entries = mealPlanEntryRepository.findByOrgIdAndDateRange(orgId, from, to);
        } else {
            entries = mealPlanEntryRepository.findByOrgId(orgId);
        }
        return entries.stream().map(this::toResponse).toList();
    }

    public MealPlanEntryResponse update(String orgId, String id, UpdateMealPlanEntryRequest request) {
        MealPlanEntry entry = findByIdAndOrg(orgId, id);
        Recipe recipe = findRecipeByIdAndOrg(orgId, request.recipeId());

        entry.setDate(request.date());
        entry.setMealType(request.mealType());
        entry.setRecipeId(recipe.getId());
        entry.setRecipeName(recipe.getName());
        entry.setServings(request.servings());
        entry.setNotes(request.notes());
        entry = mealPlanEntryRepository.save(entry);
        return toResponse(entry);
    }

    public void delete(String orgId, String id) {
        MealPlanEntry entry = findByIdAndOrg(orgId, id);
        mealPlanEntryRepository.deleteById(entry.getId());
    }

    public MealPlanEntryResponse confirm(String orgId, String id) {
        MealPlanEntry entry = findByIdAndOrg(orgId, id);
        if (entry.getStatus() == MealStatus.CONFIRMED) {
            return toResponse(entry);
        }

        Recipe recipe = recipeRepository.findById(entry.getRecipeId()).orElse(null);
        if (recipe != null && orgId.equals(recipe.getOrgId())) {
            // Local recipe — deduct inventory normally
            BigDecimal scaleFactor = BigDecimal.valueOf(entry.getServings())
                    .divide(BigDecimal.valueOf(recipe.getBaseServings()), 10, RoundingMode.HALF_UP);

            for (var ri : recipe.getIngredients()) {
                BigDecimal scaledQty = ri.getMeasurement().getQuantity()
                        .multiply(scaleFactor)
                        .setScale(2, RoundingMode.HALF_UP);

                inventoryItemRepository.findByOrgIdAndIngredientIdAndUnit(
                        orgId, ri.getIngredientId(), ri.getMeasurement().getUnit()
                ).ifPresent(item -> {
                    BigDecimal newQty = item.getQuantity().subtract(scaledQty).max(BigDecimal.ZERO);
                    item.setQuantity(newQty);
                    inventoryItemRepository.save(item);
                });
            }
        } else if (entry.getPinnedId() != null) {
            // Pinned recipe — deduct inventory from shared ingredients
            PinnedRecipe pin = pinnedRecipeRepository.findByIdAndOrgId(entry.getPinnedId(), orgId)
                    .orElse(null);
            if (pin != null) {
                // Lazy backfill ingredient links
                if (ingredientLinkerService.needsLinking(pin.getIngredients())) {
                    ingredientLinkerService.linkSharedIngredients(orgId, pin.getIngredients());
                    pinnedRecipeRepository.save(pin);
                }

                BigDecimal scaleFactor = BigDecimal.valueOf(entry.getServings())
                        .divide(BigDecimal.valueOf(pin.getBaseServings()), 10, RoundingMode.HALF_UP);

                for (SharedRecipeIngredient sri : pin.getIngredients()) {
                    if (sri.getIngredientId() == null) continue;
                    BigDecimal scaledQty = BigDecimal.valueOf(sri.getQuantity())
                            .multiply(scaleFactor)
                            .setScale(2, RoundingMode.HALF_UP);
                    try {
                        MeasurementUnit unit = MeasurementUnit.valueOf(sri.getUnit());
                        inventoryItemRepository.findByOrgIdAndIngredientIdAndUnit(
                                orgId, sri.getIngredientId(), unit
                        ).ifPresent(item -> {
                            BigDecimal newQty = item.getQuantity().subtract(scaledQty).max(BigDecimal.ZERO);
                            item.setQuantity(newQty);
                            inventoryItemRepository.save(item);
                        });
                    } catch (IllegalArgumentException e) {
                        // skip unknown unit
                    }
                }
            }
        }

        entry.setStatus(MealStatus.CONFIRMED);
        entry = mealPlanEntryRepository.save(entry);
        return toResponse(entry);
    }

    private MealPlanEntry findByIdAndOrg(String orgId, String id) {
        MealPlanEntry entry = mealPlanEntryRepository.findById(id)
                .orElseThrow(() -> new MealPlanEntryNotFoundException(id));
        if (!orgId.equals(entry.getOrgId())) {
            throw new MealPlanEntryNotFoundException(id);
        }
        return entry;
    }

    private Recipe findRecipeByIdAndOrg(String orgId, String recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        if (!orgId.equals(recipe.getOrgId())) {
            throw new RecipeNotFoundException(recipeId);
        }
        return recipe;
    }

    private List<DietaryWarning> buildDietaryWarnings(Recipe recipe) {
        List<String> ingredientIds = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredientId())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (ingredientIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Ingredient> ingredients = ingredientRepository.findAllById(ingredientIds);
        return ingredients.stream()
                .filter(i -> i.getDietaryTags() != null && !i.getDietaryTags().isEmpty())
                .map(i -> new DietaryWarning(i.getName(), i.getDietaryTags()))
                .toList();
    }

    private MealPlanEntryResponse toResponse(MealPlanEntry entry) {
        List<DietaryWarning> warnings = Collections.emptyList();
        Recipe recipe = recipeRepository.findById(entry.getRecipeId()).orElse(null);
        if (recipe != null) {
            warnings = buildDietaryWarnings(recipe);
        } else if (entry.getPinnedId() != null) {
            warnings = buildPinnedDietaryWarnings(entry.getPinnedId());
        }

        return new MealPlanEntryResponse(
                entry.getId(),
                entry.getDate(),
                entry.getMealType(),
                entry.getRecipeId(),
                entry.getRecipeName(),
                entry.getServings(),
                entry.getNotes(),
                entry.getStatus(),
                warnings
        );
    }

    private List<DietaryWarning> buildPinnedDietaryWarnings(String pinnedId) {
        PinnedRecipe pin = pinnedRecipeRepository.findById(pinnedId).orElse(null);
        if (pin == null || pin.getIngredients() == null) {
            return Collections.emptyList();
        }
        return pin.getIngredients().stream()
                .filter(i -> i.getDietaryTags() != null && !i.getDietaryTags().isEmpty())
                .map(i -> {
                    Set<DietaryTag> tags = i.getDietaryTags().stream()
                            .map(name -> {
                                try { return DietaryTag.valueOf(name); }
                                catch (IllegalArgumentException e) { return null; }
                            })
                            .filter(t -> t != null)
                            .collect(Collectors.toSet());
                    return new DietaryWarning(i.getIngredientName(), tags);
                })
                .filter(w -> !w.tags().isEmpty())
                .toList();
    }
}
