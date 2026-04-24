package com.endoran.foodplan.service;

import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.InventoryItem;
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
import java.util.List;
import java.util.Map;

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
        int skipped = 0;

        Map<String, Ingredient> canonicalMap = new HashMap<>();

        for (Ingredient ing : ingredients) {
            String normalized = IngredientAliasDictionary.resolveAndNormalize(ing.getName());

            boolean changed = false;
            if (!ing.getName().equals(normalized)) {
                renames.add(new Rename(ing.getId(), ing.getName(), normalized));
                changed = true;
            }

            String key = normalized.toLowerCase();
            Ingredient existing = canonicalMap.get(key);
            if (existing == null) {
                canonicalMap.put(key, ing);
            } else {
                Ingredient winner = pickWinner(existing, ing);
                Ingredient loser = (winner == existing) ? ing : existing;
                canonicalMap.put(key, winner);
                merges.add(new Merge(winner.getId(), winner.getName(), loser.getId(), loser.getName(), normalized));
                changed = true;
            }

            if (!changed) skipped++;
        }

        if (!dryRun) {
            for (Rename rename : renames) {
                Ingredient ing = ingredientRepository.findById(rename.ingredientId).orElse(null);
                if (ing != null) {
                    ing.setName(rename.newName);
                    ingredientRepository.save(ing);
                }
            }

            for (Merge merge : merges) {
                repointReferences(orgId, merge.loserId, merge.winnerId, merge.canonicalName);
                ingredientRepository.deleteById(merge.loserId);
                Ingredient winner = ingredientRepository.findById(merge.winnerId).orElse(null);
                if (winner != null && !winner.getName().equals(merge.canonicalName)) {
                    winner.setName(merge.canonicalName);
                    ingredientRepository.save(winner);
                }
            }
        }

        log.info("Normalization for org {}: {} renames, {} merges, {} skipped (dryRun={})",
                orgId, renames.size(), merges.size(), skipped, dryRun);

        return new NormalizationResult(renames, merges, skipped);
    }

    private Ingredient pickWinner(Ingredient a, Ingredient b) {
        if (!a.isNeedsReview() && b.isNeedsReview()) return a;
        if (a.isNeedsReview() && !b.isNeedsReview()) return b;
        return a.getId().compareTo(b.getId()) <= 0 ? a : b;
    }

    private void repointReferences(String orgId, String loserId, String winnerId, String canonicalName) {
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

        List<InventoryItem> loserItems = inventoryItemRepository.findByOrgId(orgId).stream()
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
            int skipped
    ) {}

    public record Rename(String ingredientId, String oldName, String newName) {}

    public record Merge(String winnerId, String winnerName, String loserId,
                         String loserName, String canonicalName) {}
}
