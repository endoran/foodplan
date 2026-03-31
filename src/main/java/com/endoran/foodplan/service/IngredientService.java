package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.BatchCreateIngredientsRequest;
import com.endoran.foodplan.dto.CreateIngredientRequest;
import com.endoran.foodplan.dto.IngredientPreparation;
import com.endoran.foodplan.dto.IngredientResponse;
import com.endoran.foodplan.dto.UpdateIngredientRequest;
import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public IngredientResponse create(String orgId, CreateIngredientRequest request) {
        Ingredient ingredient = new Ingredient();
        ingredient.setOrgId(orgId);
        ingredient.setName(request.name());
        ingredient.setStorageCategory(request.storageCategory());
        ingredient.setGroceryCategory(request.groceryCategory());
        if (request.dietaryTags() != null) {
            ingredient.setDietaryTags(request.dietaryTags());
        }
        ingredient.setShoppingListExclude(request.shoppingListExclude());
        ingredient = ingredientRepository.save(ingredient);
        return toResponse(ingredient);
    }

    public IngredientResponse getById(String orgId, String id) {
        Ingredient ingredient = findByIdAndOrg(orgId, id);
        return toResponse(ingredient);
    }

    public List<IngredientResponse> list(String orgId, String name, GroceryCategory groceryCategory, DietaryTag dietaryTag) {
        List<Ingredient> ingredients;
        if (name != null && !name.isBlank()) {
            ingredients = ingredientRepository.findByOrgIdAndNameContainingIgnoreCase(orgId, name);
        } else if (groceryCategory != null) {
            ingredients = ingredientRepository.findByOrgIdAndGroceryCategory(orgId, groceryCategory);
        } else if (dietaryTag != null) {
            ingredients = ingredientRepository.findByOrgIdAndDietaryTagsContaining(orgId, dietaryTag);
        } else {
            ingredients = ingredientRepository.findByOrgId(orgId);
        }
        return ingredients.stream().map(this::toResponse).toList();
    }

    public IngredientResponse update(String orgId, String id, UpdateIngredientRequest request) {
        Ingredient ingredient = findByIdAndOrg(orgId, id);
        ingredient.setName(request.name());
        ingredient.setStorageCategory(request.storageCategory());
        ingredient.setGroceryCategory(request.groceryCategory());
        ingredient.setDietaryTags(request.dietaryTags() != null ? request.dietaryTags() : Collections.emptySet());
        ingredient.setShoppingListExclude(request.shoppingListExclude());
        ingredient.setNeedsReview(false);
        ingredient = ingredientRepository.save(ingredient);
        return toResponse(ingredient);
    }

    public void delete(String orgId, String id) {
        Ingredient ingredient = findByIdAndOrg(orgId, id);
        ingredientRepository.deleteById(ingredient.getId());
    }

    public List<IngredientPreparation> prepareIngredients(String orgId, List<String> ingredientNames) {
        return ingredientNames.stream().map(name -> {
            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, name);
            if (existing.isPresent()) {
                Ingredient ing = existing.get();
                return new IngredientPreparation(
                        name, IngredientPreparation.Status.EXISTING,
                        ing.getStorageCategory(), ing.getGroceryCategory(),
                        ing.isShoppingListExclude());
            }
            IngredientCategoryInference.InferredCategories inferred =
                    IngredientCategoryInference.infer(name);
            return new IngredientPreparation(
                    name, IngredientPreparation.Status.NEW,
                    inferred.storage(), inferred.grocery(), false);
        }).toList();
    }

    public List<IngredientResponse> batchCreate(String orgId, BatchCreateIngredientsRequest request) {
        return request.ingredients().stream().map(entry -> {
            // Skip if already exists (race condition guard)
            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, entry.name());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
            Ingredient ingredient = new Ingredient();
            ingredient.setOrgId(orgId);
            ingredient.setName(entry.name());
            ingredient.setStorageCategory(entry.storageCategory());
            ingredient.setGroceryCategory(entry.groceryCategory());
            ingredient.setShoppingListExclude(entry.shoppingListExclude());
            ingredient = ingredientRepository.save(ingredient);
            return toResponse(ingredient);
        }).toList();
    }

    private Ingredient findByIdAndOrg(String orgId, String id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        if (!orgId.equals(ingredient.getOrgId())) {
            throw new IngredientNotFoundException(id);
        }
        return ingredient;
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getStorageCategory(),
                ingredient.getGroceryCategory(),
                ingredient.getDietaryTags(),
                ingredient.isNeedsReview(),
                ingredient.isShoppingListExclude()
        );
    }
}
