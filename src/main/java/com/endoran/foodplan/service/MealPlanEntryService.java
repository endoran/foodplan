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
import com.endoran.foodplan.repository.MealPlanEntryRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class MealPlanEntryService {

    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public MealPlanEntryService(MealPlanEntryRepository mealPlanEntryRepository,
                                RecipeRepository recipeRepository,
                                IngredientRepository ingredientRepository,
                                InventoryItemRepository inventoryItemRepository) {
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public MealPlanEntryResponse create(String orgId, CreateMealPlanEntryRequest request) {
        Recipe recipe = findRecipeByIdAndOrg(orgId, request.recipeId());

        MealPlanEntry entry = new MealPlanEntry();
        entry.setOrgId(orgId);
        entry.setDate(request.date());
        entry.setMealType(request.mealType());
        entry.setRecipeId(recipe.getId());
        entry.setRecipeName(recipe.getName());
        entry.setServings(request.servings());
        entry.setNotes(request.notes());
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

        Recipe recipe = findRecipeByIdAndOrg(orgId, entry.getRecipeId());
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
}
