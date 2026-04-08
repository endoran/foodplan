package com.endoran.foodplan.service;

import com.endoran.foodplan.config.SharedMongoConfig.SharedMongoHolder;
import com.endoran.foodplan.model.SharedRecipe;
import com.endoran.foodplan.repository.PinnedRecipeRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import com.mongodb.MongoTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GlobalRecipeServiceCircuitBreakerTest {

    private MongoTemplate sharedMongo;
    private GlobalRecipeService service;

    @BeforeEach
    void setUp() {
        sharedMongo = mock(MongoTemplate.class);
        PinnedRecipeRepository pinnedRepo = mock(PinnedRecipeRepository.class);
        RecipeRepository recipeRepo = mock(RecipeRepository.class);

        service = new GlobalRecipeService(
                new SharedMongoHolder(sharedMongo),
                pinnedRepo, recipeRepo,
                true, "test-instance", "Test Instance");
    }

    @Test
    void circuitOpensAfterFirstFailure() {
        when(sharedMongo.exists(any(Query.class), eq(SharedRecipe.class)))
                .thenThrow(new MongoTimeoutException("server selection timed out"));

        // First call: hits mongo, fails, opens circuit
        assertFalse(service.isRecipeShared("org1", "recipe1"));

        // Second call: circuit open, should NOT hit mongo
        long start = System.nanoTime();
        assertFalse(service.isRecipeShared("org1", "recipe2"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs < 50, "Circuit breaker should skip mongo, took " + elapsedMs + "ms");
        verify(sharedMongo, times(1)).exists(any(Query.class), eq(SharedRecipe.class));
    }

    @Test
    void circuitReclosingAfterCooldown() throws Exception {
        when(sharedMongo.exists(any(Query.class), eq(SharedRecipe.class)))
                .thenThrow(new MongoTimeoutException("timeout"))
                .thenReturn(true);

        // Trip the circuit
        assertFalse(service.isRecipeShared("org1", "recipe1"));

        // Simulate cooldown elapsed by resetting the timestamp
        var field = GlobalRecipeService.class.getDeclaredField("sharedMongoDownUntil");
        field.setAccessible(true);
        ((AtomicLong) field.get(service)).set(0);

        // Circuit closed: should hit mongo again and succeed
        assertTrue(service.isRecipeShared("org1", "recipe2"));
        verify(sharedMongo, times(2)).exists(any(Query.class), eq(SharedRecipe.class));
    }

    @Test
    void disabledServiceNeverHitsMongo() {
        GlobalRecipeService disabled = new GlobalRecipeService(
                new SharedMongoHolder(sharedMongo),
                mock(PinnedRecipeRepository.class), mock(RecipeRepository.class),
                false, "test-instance", "Test");

        assertFalse(disabled.isRecipeShared("org1", "recipe1"));
        verify(sharedMongo, never()).exists(any(Query.class), eq(SharedRecipe.class));
    }

    @Test
    void healthyMongoReturnsTrue() {
        when(sharedMongo.exists(any(Query.class), eq(SharedRecipe.class))).thenReturn(true);

        assertTrue(service.isRecipeShared("org1", "recipe1"));
        verify(sharedMongo, times(1)).exists(any(Query.class), eq(SharedRecipe.class));
    }

    @Test
    void multipleRecipesAfterFailureAllSkipMongo() {
        when(sharedMongo.exists(any(Query.class), eq(SharedRecipe.class)))
                .thenThrow(new MongoTimeoutException("timeout"));

        // First call trips the circuit
        service.isRecipeShared("org1", "r1");

        // Simulate listing 50 recipes — none should hit mongo
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            assertFalse(service.isRecipeShared("org1", "recipe-" + i));
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs < 50, "50 calls should complete instantly, took " + elapsedMs + "ms");
        verify(sharedMongo, times(1)).exists(any(Query.class), eq(SharedRecipe.class));
    }
}
