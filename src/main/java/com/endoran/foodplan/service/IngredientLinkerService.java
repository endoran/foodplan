package com.endoran.foodplan.service;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.SharedRecipeIngredient;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.repository.IngredientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IngredientLinkerService {

    private static final Logger log = LoggerFactory.getLogger(IngredientLinkerService.class);

    private final IngredientRepository ingredientRepository;

    public IngredientLinkerService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public void linkSharedIngredients(String orgId, List<SharedRecipeIngredient> ingredients) {
        for (SharedRecipeIngredient sri : ingredients) {
            if (sri.getIngredientId() != null && !sri.getIngredientId().isBlank()) continue;

            sri.setIngredientName(IngredientAliasDictionary.resolveAndNormalize(sri.getIngredientName()));
            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, sri.getIngredientName());
            if (existing.isPresent()) {
                sri.setIngredientId(existing.get().getId());
            } else {
                Ingredient newIng = new Ingredient();
                newIng.setOrgId(orgId);
                newIng.setName(sri.getIngredientName());

                // Use source metadata from shared recipe if available, fall back to inference
                if (sri.getGroceryCategory() != null) {
                    try {
                        newIng.setGroceryCategory(GroceryCategory.valueOf(sri.getGroceryCategory()));
                    } catch (IllegalArgumentException e) {
                        newIng.setGroceryCategory(IngredientCategoryInference.infer(sri.getIngredientName()).grocery());
                    }
                } else {
                    newIng.setGroceryCategory(IngredientCategoryInference.infer(sri.getIngredientName()).grocery());
                }

                if (sri.getStorageCategory() != null) {
                    try {
                        newIng.setStorageCategory(StorageCategory.valueOf(sri.getStorageCategory()));
                    } catch (IllegalArgumentException e) {
                        newIng.setStorageCategory(IngredientCategoryInference.infer(sri.getIngredientName()).storage());
                    }
                } else {
                    newIng.setStorageCategory(IngredientCategoryInference.infer(sri.getIngredientName()).storage());
                }

                // Copy dietary tags from source
                if (sri.getDietaryTags() != null && !sri.getDietaryTags().isEmpty()) {
                    Set<DietaryTag> tags = new HashSet<>();
                    for (String tagName : sri.getDietaryTags()) {
                        try {
                            tags.add(DietaryTag.valueOf(tagName));
                        } catch (IllegalArgumentException e) {
                            log.debug("Skipping unknown dietary tag '{}' for '{}'", tagName, sri.getIngredientName());
                        }
                    }
                    newIng.setDietaryTags(tags);
                }

                newIng.setNeedsReview(false);
                newIng = ingredientRepository.save(newIng);
                sri.setIngredientId(newIng.getId());
                log.info("Auto-created ingredient '{}' [grocery={}, storage={}] for org {}",
                        sri.getIngredientName(), newIng.getGroceryCategory(), newIng.getStorageCategory(), orgId);
            }
        }
    }

    public boolean needsLinking(List<SharedRecipeIngredient> ingredients) {
        return ingredients.stream()
                .anyMatch(i -> i.getIngredientId() == null || i.getIngredientId().isBlank());
    }
}
