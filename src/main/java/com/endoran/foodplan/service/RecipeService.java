package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.CreateRecipeRequest;
import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
import com.endoran.foodplan.dto.RecipeIngredientResponse;
import com.endoran.foodplan.dto.RecipeResponse;
import com.endoran.foodplan.dto.UpdateRecipeRequest;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.Recipe;
import com.endoran.foodplan.model.RecipeIngredient;
import com.endoran.foodplan.model.ScanSession;
import com.endoran.foodplan.model.TrainingPair;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import com.endoran.foodplan.repository.ScanSessionRepository;
import com.endoran.foodplan.repository.TrainingPairRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final ScanSessionRepository scanSessionRepository;
    private final TrainingPairRepository trainingPairRepository;
    private GlobalRecipeService globalRecipeService;

    public RecipeService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository,
                         ScanSessionRepository scanSessionRepository, TrainingPairRepository trainingPairRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.scanSessionRepository = scanSessionRepository;
        this.trainingPairRepository = trainingPairRepository;
    }

    @Autowired(required = false)
    @Lazy
    public void setGlobalRecipeService(GlobalRecipeService globalRecipeService) {
        this.globalRecipeService = globalRecipeService;
    }

    public RecipeResponse create(String orgId, CreateRecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.setOrgId(orgId);
        recipe.setName(request.name());
        recipe.setInstructions(request.instructions());
        recipe.setBaseServings(request.baseServings());
        recipe.setIngredients(toIngredients(request.ingredients()));
        autoCreateIngredients(orgId, recipe.getIngredients());
        recipe = recipeRepository.save(recipe);

        // Generate training pair if this recipe came from a scan
        if (request.scanSessionId() != null) {
            generateTrainingPair(orgId, request);
        }

        return toResponse(recipe, null, orgId);
    }

    private void generateTrainingPair(String orgId, CreateRecipeRequest request) {
        try {
            var sessionOpt = scanSessionRepository.findById(request.scanSessionId());
            if (sessionOpt.isEmpty()) {
                log.warn("Scan session {} not found (expired?), skipping training pair", request.scanSessionId());
                return;
            }
            ScanSession session = sessionOpt.get();

            int index = request.scanRecipeIndex() != null ? request.scanRecipeIndex() : 0;
            if (index < 0 || index >= session.getModelOutput().size()) {
                log.warn("Scan recipe index {} out of range for session {} (has {} recipes)",
                        index, session.getId(), session.getModelOutput().size());
                return;
            }

            ImportedRecipePreview modelOutput = session.getModelOutput().get(index);

            // Build corrected output from what the user actually saved
            List<ImportedIngredientPreview> correctedIngredients = request.ingredients().stream()
                    .map(ri -> new ImportedIngredientPreview(
                            ri.section(), ri.ingredientName(),
                            ri.quantity(), ri.unit().name(), null, null))
                    .collect(Collectors.toList());
            ImportedRecipePreview correctedOutput = new ImportedRecipePreview(
                    request.name(), request.instructions(), request.baseServings(),
                    correctedIngredients, modelOutput.sourceUrl());

            boolean hasCorrections = !recipePreviewsMatch(modelOutput, correctedOutput);

            TrainingPair pair = new TrainingPair();
            pair.setOrgId(orgId);
            pair.setScanSessionId(session.getId());
            pair.setImageData(session.getImageData());
            pair.setImageContentType(session.getImageContentType());
            pair.setModelOutput(modelOutput);
            pair.setCorrectedOutput(correctedOutput);
            pair.setExtractionTier(session.getExtractionTier());
            pair.setHasCorrections(hasCorrections);
            trainingPairRepository.save(pair);

            log.info("Training pair saved (session={}, corrections={}, tier={})",
                    session.getId(), hasCorrections, session.getExtractionTier());

            // Delete scan session if all recipes have been consumed
            // For simplicity, delete after first save — multi-recipe sessions
            // will create one pair per save call
            scanSessionRepository.deleteById(session.getId());

        } catch (Exception e) {
            // Training pair generation should never fail the recipe save
            log.error("Failed to generate training pair for session {}: {}",
                    request.scanSessionId(), e.getMessage());
        }
    }

    private boolean recipePreviewsMatch(ImportedRecipePreview original, ImportedRecipePreview corrected) {
        if (!Objects.equals(original.name(), corrected.name())) return false;
        if (original.baseServings() != corrected.baseServings()) return false;
        if (!Objects.equals(original.instructions(), corrected.instructions())) return false;

        List<ImportedIngredientPreview> origIngs = original.ingredients();
        List<ImportedIngredientPreview> corrIngs = corrected.ingredients();
        if (origIngs.size() != corrIngs.size()) return false;

        for (int i = 0; i < origIngs.size(); i++) {
            ImportedIngredientPreview o = origIngs.get(i);
            ImportedIngredientPreview c = corrIngs.get(i);
            if (!Objects.equals(o.name(), c.name())) return false;
            if (!Objects.equals(o.section(), c.section())) return false;
            if (!Objects.equals(o.unit(), c.unit())) return false;
            if (o.quantity() != null && c.quantity() != null) {
                if (o.quantity().compareTo(c.quantity()) != 0) return false;
            } else if (o.quantity() != c.quantity()) {
                return false;
            }
        }
        return true;
    }

    public RecipeResponse getById(String orgId, String id, Integer targetServings) {
        Recipe recipe = findByIdAndOrg(orgId, id);
        return toResponse(recipe, targetServings, orgId);
    }

    public List<RecipeResponse> list(String orgId, String name) {
        List<Recipe> recipes;
        if (name != null && !name.isBlank()) {
            recipes = recipeRepository.findByOrgIdAndNameContainingIgnoreCase(orgId, name);
        } else {
            recipes = recipeRepository.findByOrgId(orgId);
        }
        return recipes.stream().map(r -> toResponse(r, null, orgId)).toList();
    }

    public RecipeResponse update(String orgId, String id, UpdateRecipeRequest request) {
        Recipe recipe = findByIdAndOrg(orgId, id);
        recipe.setName(request.name());
        recipe.setInstructions(request.instructions());
        recipe.setBaseServings(request.baseServings());
        recipe.setIngredients(toIngredients(request.ingredients()));
        autoCreateIngredients(orgId, recipe.getIngredients());
        recipe = recipeRepository.save(recipe);
        return toResponse(recipe, null, orgId);
    }

    public void delete(String orgId, String id) {
        Recipe recipe = findByIdAndOrg(orgId, id);
        recipeRepository.deleteById(recipe.getId());
    }

    Recipe findByIdAndOrg(String orgId, String id) {
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
                        r.section(),
                        r.ingredientId(),
                        r.ingredientName(),
                        new Measurement(r.quantity(), r.unit())))
                .toList();
    }

    private RecipeResponse toResponse(Recipe recipe, Integer targetServings, String orgId) {
        int effectiveServings = (targetServings != null && targetServings > 0)
                ? targetServings : recipe.getBaseServings();
        BigDecimal scaleFactor = BigDecimal.valueOf(effectiveServings)
                .divide(BigDecimal.valueOf(recipe.getBaseServings()), 10, RoundingMode.HALF_UP);

        List<RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(ri -> toIngredientResponse(ri, scaleFactor))
                .toList();

        boolean isShared = globalRecipeService != null
                && globalRecipeService.isRecipeShared(orgId, recipe.getId());

        return new RecipeResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getInstructions(),
                recipe.getBaseServings(),
                effectiveServings,
                ingredients,
                isShared,
                RecipeDietaryLabels.compute(recipe.getIngredients().stream()
                        .map(ri -> ri.getIngredientName()).toList())
        );
    }

    private RecipeIngredientResponse toIngredientResponse(RecipeIngredient ri, BigDecimal scaleFactor) {
        BigDecimal scaledQuantity = ri.getMeasurement().getQuantity()
                .multiply(scaleFactor)
                .setScale(2, RoundingMode.HALF_UP);
        return new RecipeIngredientResponse(
                ri.getSection(),
                ri.getIngredientId(),
                ri.getIngredientName(),
                scaledQuantity,
                ri.getMeasurement().getUnit()
        );
    }

    public Map<String, Integer> backfillIngredients(String orgId) {
        List<Recipe> recipes = recipeRepository.findByOrgId(orgId);
        int recipesFixed = 0;
        int ingredientsCreated = 0;
        for (Recipe recipe : recipes) {
            boolean changed = false;
            for (RecipeIngredient ri : recipe.getIngredients()) {
                if (ri.getIngredientId() != null && !ri.getIngredientId().isBlank()) continue;
                var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, ri.getIngredientName());
                if (existing.isPresent()) {
                    ri.setIngredientId(existing.get().getId());
                    changed = true;
                } else {
                    IngredientCategoryInference.InferredCategories inferred =
                            IngredientCategoryInference.infer(ri.getIngredientName());
                    Ingredient newIng = new Ingredient();
                    newIng.setOrgId(orgId);
                    newIng.setName(ri.getIngredientName());
                    newIng.setStorageCategory(inferred.storage());
                    newIng.setGroceryCategory(inferred.grocery());
                    newIng.setNeedsReview(true);
                    newIng = ingredientRepository.save(newIng);
                    ri.setIngredientId(newIng.getId());
                    ingredientsCreated++;
                    changed = true;
                }
            }
            if (changed) {
                recipeRepository.save(recipe);
                recipesFixed++;
            }
        }
        return Map.of("recipesFixed", recipesFixed, "ingredientsCreated", ingredientsCreated);
    }

    private void autoCreateIngredients(String orgId, List<RecipeIngredient> ingredients) {
        for (RecipeIngredient ri : ingredients) {
            if (ri.getIngredientId() != null && !ri.getIngredientId().isBlank()) continue;

            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(
                    orgId, ri.getIngredientName());
            if (existing.isPresent()) {
                ri.setIngredientId(existing.get().getId());
            } else {
                IngredientCategoryInference.InferredCategories inferred =
                        IngredientCategoryInference.infer(ri.getIngredientName());
                Ingredient newIng = new Ingredient();
                newIng.setOrgId(orgId);
                newIng.setName(ri.getIngredientName());
                newIng.setStorageCategory(inferred.storage());
                newIng.setGroceryCategory(inferred.grocery());
                newIng.setNeedsReview(true);
                newIng = ingredientRepository.save(newIng);
                ri.setIngredientId(newIng.getId());
            }
        }
    }
}
