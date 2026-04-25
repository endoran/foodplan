package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.BatchCreateIngredientsRequest;
import com.endoran.foodplan.dto.BulkUpdateIngredientsRequest;
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
import java.util.Comparator;
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
        ingredient.setName(IngredientAliasDictionary.resolveAndNormalize(request.name()));
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
        ingredients.sort(Comparator.comparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER));
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
            String normalized = IngredientAliasDictionary.resolveAndNormalize(name);
            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, normalized);
            if (existing.isPresent()) {
                Ingredient ing = existing.get();
                return new IngredientPreparation(
                        normalized, IngredientPreparation.Status.EXISTING,
                        ing.getStorageCategory(), ing.getGroceryCategory(),
                        ing.getDietaryTags(), ing.isShoppingListExclude());
            }
            IngredientCategoryInference.InferredCategories inferred =
                    IngredientCategoryInference.infer(normalized);
            return new IngredientPreparation(
                    normalized, IngredientPreparation.Status.NEW,
                    inferred.storage(), inferred.grocery(),
                    inferred.dietaryTags(), false);
        }).toList();
    }

    public List<IngredientResponse> batchCreate(String orgId, BatchCreateIngredientsRequest request) {
        return request.ingredients().stream().map(entry -> {
            String normalizedName = IngredientAliasDictionary.resolveAndNormalize(entry.name());
            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, normalizedName);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
            Ingredient ingredient = new Ingredient();
            ingredient.setOrgId(orgId);
            ingredient.setName(normalizedName);
            ingredient.setStorageCategory(entry.storageCategory());
            ingredient.setGroceryCategory(entry.groceryCategory());
            ingredient.setShoppingListExclude(entry.shoppingListExclude());
            ingredient = ingredientRepository.save(ingredient);
            return toResponse(ingredient);
        }).toList();
    }

    public List<IngredientResponse> bulkUpdate(String orgId, BulkUpdateIngredientsRequest request) {
        return request.ingredients().stream().map(entry -> {
            Ingredient ingredient = findByIdAndOrg(orgId, entry.id());
            ingredient.setName(entry.name());
            ingredient.setStorageCategory(entry.storageCategory());
            ingredient.setGroceryCategory(entry.groceryCategory());
            ingredient.setDietaryTags(entry.dietaryTags() != null ? entry.dietaryTags() : Collections.emptySet());
            ingredient.setShoppingListExclude(entry.shoppingListExclude());
            ingredient.setNeedsReview(false);
            ingredient = ingredientRepository.save(ingredient);
            return toResponse(ingredient);
        }).toList();
    }

    public List<IngredientResponse> autoCategorize(String orgId) {
        List<Ingredient> ingredients = ingredientRepository.findByOrgId(orgId);
        List<Ingredient> updated = ingredients.stream()
                .filter(Ingredient::isNeedsReview)
                .peek(ing -> {
                    IngredientCategoryInference.InferredCategories inferred =
                            IngredientCategoryInference.infer(ing.getName());
                    ing.setStorageCategory(inferred.storage());
                    ing.setGroceryCategory(inferred.grocery());
                    ing.setDietaryTags(inferred.dietaryTags());
                    ing.setNeedsReview(IngredientKnowledgeBase.lookup(ing.getName()).isEmpty());
                })
                .toList();
        ingredientRepository.saveAll(updated);
        ingredients.sort(Comparator.comparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER));
        return ingredients.stream().map(this::toResponse).toList();
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
