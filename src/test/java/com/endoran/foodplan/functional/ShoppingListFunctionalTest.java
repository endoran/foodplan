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
class ShoppingListFunctionalTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MealPlanEntryRepository mealPlanEntryRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @AfterEach
    void cleanup() {
        mealPlanEntryRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void emptyCalendarReturnsEmptyList() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles", hasSize(0)));
    }

    @Test
    void singleMealGeneratesShoppingItems() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 2,
                flourId, "Flour", "2.00", MeasurementUnit.CUP);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 2);

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles", hasSize(1)))
                .andExpect(jsonPath("$.aisles[0].category").value("BAKING"))
                .andExpect(jsonPath("$.aisles[0].items", hasSize(1)))
                .andExpect(jsonPath("$.aisles[0].items[0].ingredientName").value("Flour"))
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(2.00))
                .andExpect(jsonPath("$.aisles[0].items[0].unit").value("CUP"));
    }

    @Test
    void twoMealsWithSameIngredientSumQuantities() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 1,
                flourId, "Flour", "2.00", MeasurementUnit.CUP);

        // Two meals, 1 serving each → 2 + 2 = 4 cups
        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 1);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 2), MealType.BREAKFAST, recipeId, 1);

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(4.00))
                .andExpect(jsonPath("$.aisles[0].items[0].unit").value("CUP"));
    }

    @Test
    void scaledServingsAffectQuantity() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        // Recipe: 2 cups flour for 2 servings
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 2,
                flourId, "Flour", "2.00", MeasurementUnit.CUP);

        // Meal: 6 servings → scale factor 3 → 6 cups
        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 6);

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(6.00));
    }

    @Test
    void inventorySubtractsFromDemand() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 1,
                flourId, "Flour", "5.00", MeasurementUnit.CUP);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 1);
        createInventoryItem(token, flourId, "2.00", MeasurementUnit.CUP);

        // Need 5, have 2 → buy 3
        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(3.00));
    }

    @Test
    void fullyStockedItemExcludedFromList() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 1,
                flourId, "Flour", "2.00", MeasurementUnit.CUP);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 1);
        createInventoryItem(token, flourId, "10.00", MeasurementUnit.CUP);

        // Need 2, have 10 → fully stocked, no items
        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles", hasSize(0)));
    }

    @Test
    void unitNormalizationCombinesCupsAndPints() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);

        // Recipe A: 2 cups flour (1 serving)
        String recipeA = createRecipeWithIngredient(token, "Pancakes", 1,
                flourId, "Flour", "2.00", MeasurementUnit.CUP);
        // Recipe B: 1 pint flour = 2 cups (1 serving)
        String recipeB = createRecipeWithIngredient(token, "Bread", 1,
                flourId, "Flour", "1.00", MeasurementUnit.PINT);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeA, 1);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 2), MealType.LUNCH, recipeB, 1);

        // 2 cups + 1 pint = 2 cups + 2 cups = 4 cups
        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles[0].items", hasSize(1)))
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(4.00))
                .andExpect(jsonPath("$.aisles[0].items[0].unit").value("CUP"));
    }

    @Test
    void groupedByGroceryCategory() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        String chickenId = createIngredient(token, "Chicken", GroceryCategory.MEAT);

        RecipeIngredientRequest ri1 = new RecipeIngredientRequest(null,flourId, "Flour",
                new BigDecimal("2.00"), MeasurementUnit.CUP);
        RecipeIngredientRequest ri2 = new RecipeIngredientRequest(null,chickenId, "Chicken",
                new BigDecimal("1.00"), MeasurementUnit.LBS);
        String recipeId = createRecipeWithIngredients(token, "Fried Chicken", 1, List.of(ri1, ri2));

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.DINNER, recipeId, 1);

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles", hasSize(2)));
    }

    @Test
    void crossOrgIsolation() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");

        String flourA = createIngredient(tokenA, "Flour", GroceryCategory.BAKING);
        String recipeA = createRecipeWithIngredient(tokenA, "Pancakes", 1,
                flourA, "Flour", "5.00", MeasurementUnit.CUP);
        createMealPlanEntry(tokenA, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeA, 1);

        // Org B should see empty list
        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles", hasSize(0)));
    }

    @Test
    void mealsOutsideDateRangeExcluded() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String flourId = createIngredient(token, "Flour", GroceryCategory.BAKING);
        String recipeId = createRecipeWithIngredient(token, "Pancakes", 1,
                flourId, "Flour", "2.00", MeasurementUnit.CUP);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 1);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 15), MealType.BREAKFAST, recipeId, 1);

        // Only April 1-7 range
        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(2.00));
    }

    @Test
    void nullIngredientIdsProduceSeparateItems() throws Exception {
        // Regression: recipes without ingredient linkage (null ingredientId)
        // must not collapse all items of the same unit family into one entry.
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        RecipeIngredientRequest stock = new RecipeIngredientRequest(null,
                null, "Chicken Stock", new BigDecimal("2.00"), MeasurementUnit.CUP);
        RecipeIngredientRequest cream = new RecipeIngredientRequest(null,
                null, "Heavy Cream", new BigDecimal("1.00"), MeasurementUnit.CUP);
        RecipeIngredientRequest onion = new RecipeIngredientRequest(null,
                null, "Onion", new BigDecimal("3.00"), MeasurementUnit.WHOLE);
        RecipeIngredientRequest garlic = new RecipeIngredientRequest(null,
                null, "Garlic", new BigDecimal("5.00"), MeasurementUnit.WHOLE);
        String recipeId = createRecipeWithIngredients(token, "Soup", 1,
                List.of(stock, cream, onion, garlic));

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.DINNER, recipeId, 1);

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                // 4 distinct ingredients, not collapsed by unit family
                .andExpect(jsonPath("$.aisles[*].items[*]", hasSize(4)))
                .andExpect(jsonPath("$.aisles[*].items[?(@.ingredientName == 'Chicken Stock')].quantity",
                        contains(2.00)))
                .andExpect(jsonPath("$.aisles[*].items[?(@.ingredientName == 'Heavy Cream')].quantity",
                        contains(1.00)));
    }

    @Test
    void nullIngredientIdsSameNameStillAggregate() throws Exception {
        // Two recipes both using "Onion" with null ingredientId should aggregate
        // by name since they represent the same ingredient.
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        RecipeIngredientRequest onionA = new RecipeIngredientRequest(null,
                null, "Onion", new BigDecimal("2.00"), MeasurementUnit.WHOLE);
        String recipeA = createRecipeWithIngredients(token, "Soup", 1, List.of(onionA));

        RecipeIngredientRequest onionB = new RecipeIngredientRequest(null,
                null, "Onion", new BigDecimal("3.00"), MeasurementUnit.WHOLE);
        String recipeB = createRecipeWithIngredients(token, "Stew", 1, List.of(onionB));

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.DINNER, recipeA, 1);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 2), MealType.DINNER, recipeB, 1);

        mockMvc.perform(get("/api/v1/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-07"))
                .andExpect(status().isOk())
                // Same name → one aggregated item: 2 + 3 = 5
                .andExpect(jsonPath("$.aisles[*].items[*]", hasSize(1)))
                .andExpect(jsonPath("$.aisles[0].items[0].ingredientName").value("Onion"))
                .andExpect(jsonPath("$.aisles[0].items[0].quantity").value(closeTo(5.00, 0.01)));
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

    private String createIngredient(String token, String name, GroceryCategory category) throws Exception {
        CreateIngredientRequest request = new CreateIngredientRequest(
                name, StorageCategory.PANTRY, category, Set.of(), false);
        MvcResult result = mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createRecipeWithIngredient(String token, String recipeName, int baseServings,
                                               String ingredientId, String ingredientName,
                                               String quantity, MeasurementUnit unit) throws Exception {
        RecipeIngredientRequest ri = new RecipeIngredientRequest(null,
                ingredientId, ingredientName, new BigDecimal(quantity), unit);
        CreateRecipeRequest request = new CreateRecipeRequest(recipeName, "instructions", baseServings, List.of(ri));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createRecipeWithIngredients(String token, String recipeName, int baseServings,
                                                List<RecipeIngredientRequest> ingredients) throws Exception {
        CreateRecipeRequest request = new CreateRecipeRequest(recipeName, "instructions", baseServings, ingredients);
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createInventoryItem(String token, String ingredientId, String quantity,
                                      MeasurementUnit unit) throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                ingredientId, new BigDecimal(quantity), unit);
        mockMvc.perform(post("/api/v1/inventory")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void createMealPlanEntry(String token, LocalDate date, MealType mealType,
                                      String recipeId, int servings) throws Exception {
        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                date, mealType, recipeId, servings, null);
        mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
