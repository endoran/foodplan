package com.endoran.foodplan.service;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.InventoryItem;
import com.endoran.foodplan.model.MeasurementUnit;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientNormalizationServiceTest {

    @Mock private IngredientRepository ingredientRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private MongoTemplate mongoTemplate;

    private IngredientNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new IngredientNormalizationService(ingredientRepository, inventoryItemRepository, mongoTemplate);
    }

    @Test
    void dryRunDoesNotMutate() {
        Ingredient ing = makeIngredient("1", "kosher salt", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));

        var result = service.normalizeAll("org1", true);

        assertEquals(1, result.renames().size());
        assertEquals("kosher salt", result.renames().get(0).oldName());
        assertEquals("Kosher Salt", result.renames().get(0).newName());
        verify(ingredientRepository, never()).save(any());
        verify(ingredientRepository, never()).deleteById(any());
    }

    @Test
    void renameOnlyNoDuplicates() {
        Ingredient ing = makeIngredient("1", "garlic powder", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));
        when(ingredientRepository.findById("1")).thenReturn(Optional.of(ing));

        var result = service.normalizeAll("org1", false);

        assertEquals(1, result.renames().size());
        assertEquals("Garlic Powder", result.renames().get(0).newName());
        verify(ingredientRepository, atLeastOnce()).save(ing);
        assertEquals("Garlic Powder", ing.getName());
    }

    @Test
    void mergesDuplicatesKeepsReviewedWinner() {
        Ingredient reviewed = makeIngredient("1", "Garlic Powder", false);
        Ingredient unreviewed = makeIngredient("2", "garlic powder", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(reviewed, unreviewed));
        when(ingredientRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if ("1".equals(id)) return Optional.of(reviewed);
            if ("2".equals(id)) return Optional.of(unreviewed);
            return Optional.empty();
        });
        when(inventoryItemRepository.findByOrgId("org1")).thenReturn(List.of());

        var result = service.normalizeAll("org1", false);

        assertEquals(1, result.merges().size());
        assertEquals("1", result.merges().get(0).winnerId());
        assertEquals("2", result.merges().get(0).loserId());
        verify(ingredientRepository).deleteById("2");
    }

    @Test
    void aliasResolutionTriggersRename() {
        Ingredient ing = makeIngredient("1", "scallions", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));
        when(ingredientRepository.findById("1")).thenReturn(Optional.of(ing));

        var result = service.normalizeAll("org1", false);

        assertEquals(1, result.renames().size());
        assertEquals("Green Onion", result.renames().get(0).newName());
    }

    @Test
    void alreadyNormalizedSkipped() {
        Ingredient ing = makeIngredient("1", "Chicken Breast", false);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));

        var result = service.normalizeAll("org1", false);

        assertEquals(0, result.renames().size());
        assertEquals(0, result.merges().size());
    }

    @Test
    void threeWayMergeAllLosersPointToSingleWinner() {
        Ingredient a = makeIngredient("1", "garlic powder", true);
        Ingredient b = makeIngredient("2", "Garlic Powder", false);
        Ingredient c = makeIngredient("3", "GARLIC POWDER", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(a, b, c));
        when(ingredientRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if ("1".equals(id)) return Optional.of(a);
            if ("2".equals(id)) return Optional.of(b);
            if ("3".equals(id)) return Optional.of(c);
            return Optional.empty();
        });
        when(inventoryItemRepository.findByOrgId("org1")).thenReturn(List.of());

        var result = service.normalizeAll("org1", false);

        assertEquals(2, result.merges().size());
        for (var merge : result.merges()) {
            assertEquals("2", merge.winnerId());
        }
        var loserIds = result.merges().stream().map(m -> m.loserId()).sorted().toList();
        assertEquals(List.of("1", "3"), loserIds);
        verify(ingredientRepository).deleteById("1");
        verify(ingredientRepository).deleteById("3");
    }

    @Test
    void inventoryMergesSameUnitSumsQuantities() {
        Ingredient reviewed = makeIngredient("1", "Garlic Powder", false);
        Ingredient unreviewed = makeIngredient("2", "garlic powder", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(reviewed, unreviewed));
        when(ingredientRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if ("1".equals(id)) return Optional.of(reviewed);
            if ("2".equals(id)) return Optional.of(unreviewed);
            return Optional.empty();
        });

        InventoryItem winnerItem = new InventoryItem();
        winnerItem.setId("inv1");
        winnerItem.setOrgId("org1");
        winnerItem.setIngredientId("1");
        winnerItem.setUnit(MeasurementUnit.TSP);
        winnerItem.setQuantity(new BigDecimal("2.5"));

        InventoryItem loserItem = new InventoryItem();
        loserItem.setId("inv2");
        loserItem.setOrgId("org1");
        loserItem.setIngredientId("2");
        loserItem.setUnit(MeasurementUnit.TSP);
        loserItem.setQuantity(new BigDecimal("1.5"));

        when(inventoryItemRepository.findByOrgId("org1")).thenReturn(List.of(winnerItem, loserItem));
        when(inventoryItemRepository.findByOrgIdAndIngredientIdAndUnit("org1", "1", MeasurementUnit.TSP))
                .thenReturn(Optional.of(winnerItem));

        service.normalizeAll("org1", false);

        assertEquals(new BigDecimal("4.0"), winnerItem.getQuantity());
        verify(inventoryItemRepository).save(winnerItem);
        verify(inventoryItemRepository).deleteById("inv2");
    }

    @Test
    void mergeRepointsRecipeAndPinnedRecipeReferences() {
        Ingredient reviewed = makeIngredient("1", "Garlic Powder", false);
        Ingredient unreviewed = makeIngredient("2", "garlic powder", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(reviewed, unreviewed));
        when(ingredientRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if ("1".equals(id)) return Optional.of(reviewed);
            if ("2".equals(id)) return Optional.of(unreviewed);
            return Optional.empty();
        });
        when(inventoryItemRepository.findByOrgId("org1")).thenReturn(List.of());

        service.normalizeAll("org1", false);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<String> collectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(mongoTemplate, times(2)).updateMulti(
                queryCaptor.capture(), updateCaptor.capture(), collectionCaptor.capture());

        var collections = collectionCaptor.getAllValues();
        assertTrue(collections.contains("recipes"));
        assertTrue(collections.contains("pinned_recipes"));
    }

    private Ingredient makeIngredient(String id, String name, boolean needsReview) {
        Ingredient ing = new Ingredient();
        ing.setId(id);
        ing.setOrgId("org1");
        ing.setName(name);
        ing.setNeedsReview(needsReview);
        ing.setGroceryCategory(GroceryCategory.PRODUCE);
        ing.setStorageCategory(StorageCategory.PANTRY);
        return ing;
    }

    @Test
    void categoryFixesReportedInDryRun() {
        Ingredient ing = makeIngredient("1", "Parmesan Cheese", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));

        var result = service.normalizeAll("org1", true);

        assertEquals(1, result.categoryFixes().size());
        var fix = result.categoryFixes().get(0);
        assertEquals("Parmesan Cheese", fix.ingredientName());
        assertEquals(GroceryCategory.PRODUCE, fix.oldGrocery());
        assertEquals(GroceryCategory.DAIRY, fix.newGrocery());
        assertEquals(StorageCategory.PANTRY, fix.oldStorage());
        assertEquals(StorageCategory.REFRIGERATED, fix.newStorage());
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void categoryFixesAppliedOnExecute() {
        Ingredient ing = makeIngredient("1", "Parmesan Cheese", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));
        when(ingredientRepository.findById("1")).thenReturn(Optional.of(ing));

        service.normalizeAll("org1", false);

        assertEquals(GroceryCategory.DAIRY, ing.getGroceryCategory());
        assertEquals(StorageCategory.REFRIGERATED, ing.getStorageCategory());
        assertFalse(ing.isNeedsReview());
        assertFalse(ing.getDietaryTags().isEmpty());
        verify(ingredientRepository).save(ing);
    }

    @Test
    void manuallyReviewedIngredientsNotFixed() {
        Ingredient ing = makeIngredient("1", "Parmesan Cheese", false);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(ing));

        var result = service.normalizeAll("org1", false);

        assertEquals(0, result.categoryFixes().size());
    }

    @Test
    void mergedLosersExcludedFromCategoryFixes() {
        Ingredient winner = makeIngredient("1", "Garlic Powder", false);
        Ingredient loser = makeIngredient("2", "garlic powder", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(winner, loser));
        when(ingredientRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if ("1".equals(id)) return Optional.of(winner);
            if ("2".equals(id)) return Optional.of(loser);
            return Optional.empty();
        });
        when(inventoryItemRepository.findByOrgId("org1")).thenReturn(List.of());

        var result = service.normalizeAll("org1", false);

        for (var fix : result.categoryFixes()) {
            assertNotEquals("2", fix.ingredientId());
        }
    }
}
