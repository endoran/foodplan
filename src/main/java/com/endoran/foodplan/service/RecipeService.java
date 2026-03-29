package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.CreateRecipeRequest;
import com.endoran.foodplan.dto.RecipeIngredientResponse;
import com.endoran.foodplan.dto.RecipeResponse;
import com.endoran.foodplan.dto.UpdateRecipeRequest;
import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.Recipe;
import com.endoran.foodplan.model.RecipeIngredient;
import com.endoran.foodplan.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public RecipeResponse create(String orgId, CreateRecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.setOrgId(orgId);
        recipe.setName(request.name());
        recipe.setInstructions(request.instructions());
        recipe.setBaseServings(request.baseServings());
        recipe.setIngredients(toIngredients(request.ingredients()));
        recipe = recipeRepository.save(recipe);
        return toResponse(recipe, null);
    }

    public RecipeResponse getById(String orgId, String id, Integer targetServings) {
        Recipe recipe = findByIdAndOrg(orgId, id);
        return toResponse(recipe, targetServings);
    }

    public List<RecipeResponse> list(String orgId, String name) {
        List<Recipe> recipes;
        if (name != null && !name.isBlank()) {
            recipes = recipeRepository.findByOrgIdAndNameContainingIgnoreCase(orgId, name);
        } else {
            recipes = recipeRepository.findByOrgId(orgId);
        }
        return recipes.stream().map(r -> toResponse(r, null)).toList();
    }

    public RecipeResponse update(String orgId, String id, UpdateRecipeRequest request) {
        Recipe recipe = findByIdAndOrg(orgId, id);
        recipe.setName(request.name());
        recipe.setInstructions(request.instructions());
        recipe.setBaseServings(request.baseServings());
        recipe.setIngredients(toIngredients(request.ingredients()));
        recipe = recipeRepository.save(recipe);
        return toResponse(recipe, null);
    }

    public void delete(String orgId, String id) {
        Recipe recipe = findByIdAndOrg(orgId, id);
        recipeRepository.deleteById(recipe.getId());
    }

    private Recipe findByIdAndOrg(String orgId, String id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        if (!orgId.equals(recipe.getOrgId())) {
            throw new RecipeNotFoundException(id);
        }
        return recipe;
    }

    private List<RecipeIngredient> toIngredients(List<com.endoran.foodplan.dto.RecipeIngredientRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(r -> new RecipeIngredient(
                        r.ingredientId(),
                        r.ingredientName(),
                        new Measurement(r.quantity(), r.unit())))
                .toList();
    }

    private RecipeResponse toResponse(Recipe recipe, Integer targetServings) {
        int effectiveServings = (targetServings != null && targetServings > 0)
                ? targetServings : recipe.getBaseServings();
        BigDecimal scaleFactor = BigDecimal.valueOf(effectiveServings)
                .divide(BigDecimal.valueOf(recipe.getBaseServings()), 10, RoundingMode.HALF_UP);

        List<RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(ri -> toIngredientResponse(ri, scaleFactor))
                .toList();

        return new RecipeResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getInstructions(),
                recipe.getBaseServings(),
                effectiveServings,
                ingredients
        );
    }

    private RecipeIngredientResponse toIngredientResponse(RecipeIngredient ri, BigDecimal scaleFactor) {
        BigDecimal scaledQuantity = ri.getMeasurement().getQuantity()
                .multiply(scaleFactor)
                .setScale(2, RoundingMode.HALF_UP);
        return new RecipeIngredientResponse(
                ri.getIngredientId(),
                ri.getIngredientName(),
                scaledQuantity,
                ri.getMeasurement().getUnit()
        );
    }
}
