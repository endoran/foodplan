package com.endoran.foodplan.service;

import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.InventoryItem;
import com.endoran.foodplan.model.MeasurementUnit;
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
        verify(ingredientRepository).save(ing);
        assertEquals("Garlic Powder", ing.getName());
    }

    @Test
    void mergesDuplicatesKeepsReviewedWinner() {
        Ingredient reviewed = makeIngredient("1", "Garlic Powder", false);
        Ingredient unreviewed = makeIngredient("2", "garlic powder", true);
        when(ingredientRepository.findByOrgId("org1")).thenReturn(List.of(reviewed, unreviewed));
        when(ingredientRepository.findById("1")).thenReturn(Optional.of(reviewed));
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

    private Ingredient makeIngredient(String id, String name, boolean needsReview) {
        Ingredient ing = new Ingredient();
        ing.setId(id);
        ing.setOrgId("org1");
        ing.setName(name);
        ing.setNeedsReview(needsReview);
        ing.setGroceryCategory(GroceryCategory.PRODUCE);
        return ing;
    }
}
