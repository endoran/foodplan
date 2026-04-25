package com.endoran.foodplan.service;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.InventoryItem;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class IngredientNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(IngredientNormalizationService.class);

    private final IngredientRepository ingredientRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final MongoTemplate mongoTemplate;

    public IngredientNormalizationService(IngredientRepository ingredientRepository,
                                          InventoryItemRepository inventoryItemRepository,
                                          MongoTemplate mongoTemplate) {
        this.ingredientRepository = ingredientRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public NormalizationResult normalizeAll(String orgId, boolean dryRun) {
        List<Ingredient> ingredients = ingredientRepository.findByOrgId(orgId);
        List<Rename> renames = new ArrayList<>();
        List<Merge> merges = new ArrayList<>();
        List<CategoryFix> categoryFixes = new ArrayList<>();
        int skipped = 0;

        Map<String, List<Ingredient>> groups = new HashMap<>();

        for (Ingredient ing : ingredients) {
            String normalized = IngredientAliasDictionary.resolveAndNormalize(ing.getName());

            if (!ing.getName().equals(normalized)) {
                renames.add(new Rename(ing.getId(), ing.getName(), normalized));
            }

            String key = normalized.toLowerCase();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ing);
        }

        for (Map.Entry<String, List<Ingredient>> entry : groups.entrySet()) {
            List<Ingredient> group = entry.getValue();
            if (group.size() < 2) {
                if (renames.stream().noneMatch(r -> r.ingredientId.equals(group.get(0).getId()))) {
                    skipped++;
                }
                continue;
            }

            String canonicalName = IngredientAliasDictionary.resolveAndNormalize(group.get(0).getName());
            Ingredient winner = group.stream().reduce(this::pickWinner).orElseThrow();

            for (Ingredient loser : group) {
                if (loser == winner) continue;
                merges.add(new Merge(winner.getId(), winner.getName(), loser.getId(), loser.getName(), canonicalName));
            }
        }

        // Category fix pass: re-enrich needsReview ingredients using KB
        Set<String> mergedLoserIds = new HashSet<>();
        for (Merge merge : merges) {
            mergedLoserIds.add(merge.loserId);
        }

        for (Ingredient ing : ingredients) {
            if (mergedLoserIds.contains(ing.getId())) continue;
            if (!ing.isNeedsReview()) continue;

            String lookupName = renames.stream()
                    .filter(r -> r.ingredientId.equals(ing.getId()))
                    .map(Rename::newName)
                    .findFirst()
                    .orElse(ing.getName());

            var profile = IngredientKnowledgeBase.lookup(lookupName);
            if (profile.isEmpty()) continue;

            var p = profile.get();
            boolean categoryChanged = ing.getStorageCategory() != p.storage()
                    || ing.getGroceryCategory() != p.grocery();
            boolean tagsChanged = !p.dietaryTags().equals(ing.getDietaryTags() != null
                    ? ing.getDietaryTags() : Set.of());

            if (categoryChanged || tagsChanged) {
                categoryFixes.add(new CategoryFix(
                        ing.getId(), lookupName,
                        ing.getStorageCategory(), p.storage(),
                        ing.getGroceryCategory(), p.grocery(),
                        ing.getDietaryTags() != null ? ing.getDietaryTags() : Set.of(),
                        p.dietaryTags()));
            }
        }

        if (!dryRun) {
            for (Rename rename : renames) {
                Ingredient ing = ingredientRepository.findById(rename.ingredientId).orElse(null);
                if (ing != null) {
                    ing.setName(rename.newName);
                    ingredientRepository.save(ing);
                }
            }

            if (!merges.isEmpty()) {
                List<InventoryItem> allInventory = inventoryItemRepository.findByOrgId(orgId);

                for (Merge merge : merges) {
                    repointReferences(orgId, merge.loserId, merge.winnerId, merge.canonicalName, allInventory);
                    ingredientRepository.deleteById(merge.loserId);
                    Ingredient winner = ingredientRepository.findById(merge.winnerId).orElse(null);
                    if (winner != null && !winner.getName().equals(merge.canonicalName)) {
                        winner.setName(merge.canonicalName);
                        ingredientRepository.save(winner);
                    }
                }
            }

            for (CategoryFix fix : categoryFixes) {
                Ingredient ing = ingredientRepository.findById(fix.ingredientId).orElse(null);
                if (ing != null) {
                    ing.setStorageCategory(fix.newStorage);
                    ing.setGroceryCategory(fix.newGrocery);
                    ing.setDietaryTags(fix.newTags);
                    ing.setNeedsReview(false);
                    ingredientRepository.save(ing);
                }
            }
        }

        log.info("Normalization for org {}: {} renames, {} merges, {} category fixes, {} skipped (dryRun={})",
                orgId, renames.size(), merges.size(), categoryFixes.size(), skipped, dryRun);

        return new NormalizationResult(renames, merges, categoryFixes, skipped);
    }

    private Ingredient pickWinner(Ingredient a, Ingredient b) {
        if (!a.isNeedsReview() && b.isNeedsReview()) return a;
        if (a.isNeedsReview() && !b.isNeedsReview()) return b;
        return a.getId().compareTo(b.getId()) <= 0 ? a : b;
    }

    private void repointReferences(String orgId, String loserId, String winnerId,
                                     String canonicalName, List<InventoryItem> allInventory) {
        Query recipeQuery = new Query(Criteria.where("orgId").is(orgId)
                .and("ingredients.ingredientId").is(loserId));
        Update recipeUpdate = new Update()
                .set("ingredients.$[elem].ingredientId", winnerId)
                .set("ingredients.$[elem].ingredientName", canonicalName)
                .filterArray(Criteria.where("elem.ingredientId").is(loserId));
        mongoTemplate.updateMulti(recipeQuery, recipeUpdate, "recipes");

        Query pinnedQuery = new Query(Criteria.where("orgId").is(orgId)
                .and("ingredients.ingredientId").is(loserId));
        Update pinnedUpdate = new Update()
                .set("ingredients.$[elem].ingredientId", winnerId)
                .set("ingredients.$[elem].ingredientName", canonicalName)
                .filterArray(Criteria.where("elem.ingredientId").is(loserId));
        mongoTemplate.updateMulti(pinnedQuery, pinnedUpdate, "pinned_recipes");

        List<InventoryItem> loserItems = allInventory.stream()
                .filter(item -> loserId.equals(item.getIngredientId()))
                .toList();

        for (InventoryItem loserItem : loserItems) {
            var winnerItem = inventoryItemRepository.findByOrgIdAndIngredientIdAndUnit(
                    orgId, winnerId, loserItem.getUnit());
            if (winnerItem.isPresent()) {
                InventoryItem w = winnerItem.get();
                w.setQuantity(w.getQuantity().add(loserItem.getQuantity() != null
                        ? loserItem.getQuantity() : BigDecimal.ZERO));
                inventoryItemRepository.save(w);
                inventoryItemRepository.deleteById(loserItem.getId());
            } else {
                loserItem.setIngredientId(winnerId);
                loserItem.setIngredientName(canonicalName);
                inventoryItemRepository.save(loserItem);
            }
        }
    }

    public record NormalizationResult(
            List<Rename> renames,
            List<Merge> merges,
            List<CategoryFix> categoryFixes,
            int skipped
    ) {}

    public record Rename(String ingredientId, String oldName, String newName) {}

    public record Merge(String winnerId, String winnerName, String loserId,
                         String loserName, String canonicalName) {}

    public record CategoryFix(
            String ingredientId,
            String ingredientName,
            StorageCategory oldStorage, StorageCategory newStorage,
            GroceryCategory oldGrocery, GroceryCategory newGrocery,
            Set<DietaryTag> oldTags, Set<DietaryTag> newTags
    ) {}
}
