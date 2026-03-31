package com.endoran.foodplan.functional;

import com.endoran.foodplan.dto.*;
import com.endoran.foodplan.model.*;
import com.endoran.foodplan.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryFunctionalTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private MealPlanEntryRepository mealPlanEntryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @AfterEach
    void cleanup() {
        inventoryItemRepository.deleteAll();
        mealPlanEntryRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // --- Inventory CRUD Tests ---

    @Test
    void createInventoryItemReturns201() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String ingredientId = createIngredient(token, "Flour");

        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                ingredientId, new BigDecimal("10.00"), MeasurementUnit.CUP);

        mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ingredientId").value(ingredientId))
                .andExpect(jsonPath("$.ingredientName").value("Flour"))
                .andExpect(jsonPath("$.quantity").value(10.00))
                .andExpect(jsonPath("$.unit").value("CUP"));
    }

    @Test
    void getInventoryItemById() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String ingredientId = createIngredient(token, "Sugar");
        String itemId = createInventoryItem(token, ingredientId, "5.00", MeasurementUnit.CUP);

        mockMvc.perform(get("/api/v1/inventory/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientName").value("Sugar"))
                .andExpect(jsonPath("$.quantity").value(5.00));
    }

    @Test
    void listInventoryItems() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flour = createIngredient(token, "Flour");
        String sugar = createIngredient(token, "Sugar");
        createInventoryItem(token, flour, "10.00", MeasurementUnit.CUP);
        createInventoryItem(token, sugar, "5.00", MeasurementUnit.CUP);

        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void updateInventoryItem() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String ingredientId = createIngredient(token, "Butter");
        String itemId = createInventoryItem(token, ingredientId, "2.00", MeasurementUnit.LBS);

        UpdateInventoryItemRequest update = new UpdateInventoryItemRequest(
                new BigDecimal("3.50"), MeasurementUnit.LBS);

        mockMvc.perform(put("/api/v1/inventory/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3.50))
                .andExpect(jsonPath("$.unit").value("LBS"));
    }

    @Test
    void deleteInventoryItem() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String ingredientId = createIngredient(token, "Eggs");
        String itemId = createInventoryItem(token, ingredientId, "12.00", MeasurementUnit.WHOLE);

        mockMvc.perform(delete("/api/v1/inventory/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/inventory/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithInvalidIngredientReturns404() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "bogus-id", new BigDecimal("1.00"), MeasurementUnit.CUP);

        mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- Multi-Tenant Isolation ---

    @Test
    void getInventoryFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String ingredientId = createIngredient(tokenA, "Salt");
        String itemId = createInventoryItem(tokenA, ingredientId, "1.00", MeasurementUnit.TBSP);

        mockMvc.perform(get("/api/v1/inventory/" + itemId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOnlyReturnsOwnOrgInventory() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String ingA = createIngredient(tokenA, "Flour A");
        String ingB = createIngredient(tokenB, "Flour B");
        createInventoryItem(tokenA, ingA, "10.00", MeasurementUnit.CUP);
        createInventoryItem(tokenA, ingA, "5.00", MeasurementUnit.LBS);
        createInventoryItem(tokenB, ingB, "3.00", MeasurementUnit.CUP);

        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- Meal Confirmation + Deduction Tests ---

    @Test
    void confirmMealDeductsFromInventory() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour");
        String itemId = createInventoryItem(token, flourId, "10.00", MeasurementUnit.CUP);

        // Recipe: 2 cups flour, baseServings=2
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 2, flourId, "Flour",
                new BigDecimal("2.00"), MeasurementUnit.CUP);

        // Meal plan: 4 servings → scale factor 2 → deduct 4 cups
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1),
                MealType.BREAKFAST, recipeId, 4);

        mockMvc.perform(post("/api/v1/meal-plan/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Inventory should be 10 - 4 = 6
        mockMvc.perform(get("/api/v1/inventory/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(6.00));
    }

    @Test
    void confirmMealIsIdempotent() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour");
        createInventoryItem(token, flourId, "10.00", MeasurementUnit.CUP);

        String recipeId = createRecipeWithIngredient(token, "Bread", 1, flourId, "Flour",
                new BigDecimal("3.00"), MeasurementUnit.CUP);
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1),
                MealType.LUNCH, recipeId, 1);

        // First confirm
        mockMvc.perform(post("/api/v1/meal-plan/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Second confirm — should NOT deduct again
        mockMvc.perform(post("/api/v1/meal-plan/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Inventory should be 10 - 3 = 7, not 10 - 6 = 4
        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(7.00));
    }

    @Test
    void confirmMealFloorsAtZero() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour");
        createInventoryItem(token, flourId, "1.00", MeasurementUnit.CUP);

        // Recipe needs 5 cups but only 1 in stock
        String recipeId = createRecipeWithIngredient(token, "Big Batch", 1, flourId, "Flour",
                new BigDecimal("5.00"), MeasurementUnit.CUP);
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1),
                MealType.DINNER, recipeId, 1);

        mockMvc.perform(post("/api/v1/meal-plan/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Should be 0, not negative
        mockMvc.perform(get("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(0));
    }

    @Test
    void confirmMealWithNoInventoryDoesNotFail() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour");
        // No inventory item created

        String recipeId = createRecipeWithIngredient(token, "Cake", 1, flourId, "Flour",
                new BigDecimal("2.00"), MeasurementUnit.CUP);
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1),
                MealType.SNACK, recipeId, 1);

        mockMvc.perform(post("/api/v1/meal-plan/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void newMealPlanEntryHasPlannedStatus() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Toast", 1);

        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 1, null);

        mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    // --- Helpers ---

    private String registerAndGetToken(String email, String password, String orgName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, orgName);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.accessToken();
    }

    private String createIngredient(String token, String name) throws Exception {
        CreateIngredientRequest request = new CreateIngredientRequest(
                name, StorageCategory.DRY, GroceryCategory.BAKING, Set.of(), false);
        MvcResult result = mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createRecipe(String token, String name, int baseServings) throws Exception {
        CreateRecipeRequest request = new CreateRecipeRequest(name, "instructions", baseServings, List.of());
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createRecipeWithIngredient(String token, String recipeName, int baseServings,
                                               String ingredientId, String ingredientName,
                                               BigDecimal quantity, MeasurementUnit unit) throws Exception {
        RecipeIngredientRequest ri = new RecipeIngredientRequest(null,ingredientId, ingredientName, quantity, unit);
        CreateRecipeRequest request = new CreateRecipeRequest(recipeName, "instructions", baseServings, List.of(ri));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createInventoryItem(String token, String ingredientId, String quantity,
                                        MeasurementUnit unit) throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                ingredientId, new BigDecimal(quantity), unit);
        MvcResult result = mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createMealPlanEntry(String token, LocalDate date, MealType mealType,
                                        String recipeId, int servings) throws Exception {
        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                date, mealType, recipeId, servings, null);
        MvcResult result = mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
