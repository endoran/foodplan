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
class MealPlanEntryFunctionalTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MealPlanEntryRepository mealPlanEntryRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @AfterEach
    void cleanup() {
        mealPlanEntryRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // --- CRUD Tests ---

    @Test
    void createMealPlanEntryReturns201() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Pancakes", 4);

        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                LocalDate.of(2026, 3, 30), MealType.BREAKFAST, recipeId, 4, "Sunday brunch");

        mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.date").value("2026-03-30"))
                .andExpect(jsonPath("$.mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.recipeId").value(recipeId))
                .andExpect(jsonPath("$.recipeName").value("Pancakes"))
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.notes").value("Sunday brunch"))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void getMealPlanEntryById() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Pasta", 2);
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.DINNER, recipeId, 2, null);

        mockMvc.perform(get("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entryId))
                .andExpect(jsonPath("$.recipeName").value("Pasta"))
                .andExpect(jsonPath("$.mealType").value("DINNER"));
    }

    @Test
    void updateMealPlanEntry() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Salad", 2);
        String newRecipeId = createRecipe(token, "Soup", 4);
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.LUNCH, recipeId, 2, null);

        UpdateMealPlanEntryRequest update = new UpdateMealPlanEntryRequest(
                LocalDate.of(2026, 4, 2), MealType.DINNER, newRecipeId, 6, "Changed plan");

        mockMvc.perform(put("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-04-02"))
                .andExpect(jsonPath("$.mealType").value("DINNER"))
                .andExpect(jsonPath("$.recipeName").value("Soup"))
                .andExpect(jsonPath("$.servings").value(6))
                .andExpect(jsonPath("$.notes").value("Changed plan"));
    }

    @Test
    void deleteMealPlanEntry() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Toast", 1);
        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.SNACK, recipeId, 1, null);

        mockMvc.perform(delete("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNonexistentEntryReturns404() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        mockMvc.perform(get("/api/v1/meal-plan/nonexistent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithInvalidRecipeReturns404() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                LocalDate.of(2026, 4, 1), MealType.LUNCH, "bogus-recipe-id", 2, null);

        mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithInvalidServingsReturns400() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Omelette", 1);

        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 0, null);

        mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- Date Range Query Tests ---

    @Test
    void listByDateRange() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Rice", 2);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.LUNCH, recipeId, 2, null);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 3), MealType.DINNER, recipeId, 4, null);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 10), MealType.LUNCH, recipeId, 2, null);

        mockMvc.perform(get("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listByDateRangeAndMealType() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Eggs", 1);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.BREAKFAST, recipeId, 1, null);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.DINNER, recipeId, 2, null);
        createMealPlanEntry(token, LocalDate.of(2026, 4, 2), MealType.BREAKFAST, recipeId, 1, null);

        mockMvc.perform(get("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-02")
                        .param("mealType", "BREAKFAST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$[1].mealType").value("BREAKFAST"));
    }

    @Test
    void listAllEntriesWhenNoDateRange() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String recipeId = createRecipe(token, "Soup", 2);

        createMealPlanEntry(token, LocalDate.of(2026, 4, 1), MealType.LUNCH, recipeId, 2, null);
        createMealPlanEntry(token, LocalDate.of(2026, 5, 15), MealType.DINNER, recipeId, 4, null);

        mockMvc.perform(get("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- Dietary Warning Tests ---

    @Test
    void entryShowsDietaryWarningsFromIngredients() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String ingredientId = createIngredient(token, "Almond Milk",
                Set.of(DietaryTag.DAIRY_FREE, DietaryTag.VEGAN));
        String recipeId = createRecipeWithIngredient(token, "Smoothie", 1, ingredientId, "Almond Milk");

        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1),
                MealType.BREAKFAST, recipeId, 1, null);

        mockMvc.perform(get("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings", hasSize(1)))
                .andExpect(jsonPath("$.warnings[0].ingredientName").value("Almond Milk"))
                .andExpect(jsonPath("$.warnings[0].tags", hasSize(2)));
    }

    @Test
    void entryWithNoTaggedIngredientsHasEmptyWarnings() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String ingredientId = createIngredient(token, "Salt", Set.of());
        String recipeId = createRecipeWithIngredient(token, "Salted Water", 1, ingredientId, "Salt");

        String entryId = createMealPlanEntry(token, LocalDate.of(2026, 4, 1),
                MealType.LUNCH, recipeId, 1, null);

        mockMvc.perform(get("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    // --- Multi-Tenant Isolation Tests ---

    @Test
    void getEntryFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String recipeId = createRecipe(tokenA, "Secret Recipe", 2);
        String entryId = createMealPlanEntry(tokenA, LocalDate.of(2026, 4, 1),
                MealType.DINNER, recipeId, 2, null);

        mockMvc.perform(get("/api/v1/meal-plan/" + entryId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotCreateEntryWithOtherOrgsRecipe() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String recipeIdA = createRecipe(tokenA, "Private Recipe", 2);

        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(
                LocalDate.of(2026, 4, 1), MealType.DINNER, recipeIdA, 2, null);

        mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOnlyReturnsOwnOrgEntries() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String recipeA = createRecipe(tokenA, "Recipe A", 2);
        String recipeB = createRecipe(tokenB, "Recipe B", 2);

        createMealPlanEntry(tokenA, LocalDate.of(2026, 4, 1), MealType.LUNCH, recipeA, 2, null);
        createMealPlanEntry(tokenA, LocalDate.of(2026, 4, 2), MealType.DINNER, recipeA, 4, null);
        createMealPlanEntry(tokenB, LocalDate.of(2026, 4, 1), MealType.LUNCH, recipeB, 2, null);

        mockMvc.perform(get("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- Reference Data Test ---

    @Test
    void mealTypesEndpointReturnsAllTypes() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        mockMvc.perform(get("/api/v1/reference/meal-types")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0]").value("BREAKFAST"))
                .andExpect(jsonPath("$[3]").value("SNACK"));
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
                                               String ingredientId, String ingredientName) throws Exception {
        RecipeIngredientRequest ri = new RecipeIngredientRequest(null,
                ingredientId, ingredientName, BigDecimal.ONE, MeasurementUnit.CUP);
        CreateRecipeRequest request = new CreateRecipeRequest(recipeName, "instructions", baseServings, List.of(ri));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createIngredient(String token, String name, Set<DietaryTag> tags) throws Exception {
        CreateIngredientRequest request = new CreateIngredientRequest(
                name, StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, tags, false);
        MvcResult result = mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createMealPlanEntry(String token, LocalDate date, MealType mealType,
                                        String recipeId, int servings, String notes) throws Exception {
        CreateMealPlanEntryRequest request = new CreateMealPlanEntryRequest(date, mealType, recipeId, servings, notes);
        MvcResult result = mockMvc.perform(post("/api/v1/meal-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
