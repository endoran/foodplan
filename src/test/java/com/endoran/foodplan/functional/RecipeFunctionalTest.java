package com.endoran.foodplan.functional;

import com.endoran.foodplan.dto.AuthResponse;
import com.endoran.foodplan.dto.CreateRecipeRequest;
import com.endoran.foodplan.dto.RecipeIngredientRequest;
import com.endoran.foodplan.dto.RegisterRequest;
import com.endoran.foodplan.dto.UpdateRecipeRequest;
import com.endoran.foodplan.model.*;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.OrganizationRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import com.endoran.foodplan.repository.UserRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @AfterEach
    void cleanup() {
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // ===== Repository-level tests (existing) =====

    @Test
    void applicationContextLoads() {
    }

    @Test
    void createAndRetrieveIngredient() {
        Ingredient cheese = new Ingredient();
        cheese.setName("Cheddar Cheese");
        cheese.setGroceryCategory(GroceryCategory.DAIRY);
        cheese.setStorageCategory(StorageCategory.REFRIGERATED);

        Ingredient saved = ingredientRepository.save(cheese);
        assertNotNull(saved.getId());

        Ingredient found = ingredientRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Cheddar Cheese", found.getName());
    }

    @Test
    void ingredientSearchByName() {
        Ingredient cheddar = new Ingredient();
        cheddar.setName("Cheddar Cheese");
        cheddar.setGroceryCategory(GroceryCategory.DAIRY);
        cheddar.setStorageCategory(StorageCategory.REFRIGERATED);

        Ingredient mozzarella = new Ingredient();
        mozzarella.setName("Mozzarella Cheese");
        mozzarella.setGroceryCategory(GroceryCategory.DAIRY);
        mozzarella.setStorageCategory(StorageCategory.REFRIGERATED);

        Ingredient lettuce = new Ingredient();
        lettuce.setName("Romaine Lettuce");
        lettuce.setGroceryCategory(GroceryCategory.PRODUCE);
        lettuce.setStorageCategory(StorageCategory.FRESH);

        ingredientRepository.saveAll(List.of(cheddar, mozzarella, lettuce));

        List<Ingredient> cheeses = ingredientRepository.findByNameContainingIgnoreCase("cheese");
        assertEquals(2, cheeses.size());

        List<Ingredient> produce = ingredientRepository.findByGroceryCategory(GroceryCategory.PRODUCE);
        assertEquals(1, produce.size());
    }

    @Test
    void createFullRecipeWithIngredients() {
        Ingredient beef = new Ingredient();
        beef.setName("Ground Beef");
        beef.setGroceryCategory(GroceryCategory.MEAT);
        beef.setStorageCategory(StorageCategory.REFRIGERATED);
        beef = ingredientRepository.save(beef);

        Ingredient cheese = new Ingredient();
        cheese.setName("Cheddar Cheese");
        cheese.setGroceryCategory(GroceryCategory.DAIRY);
        cheese.setStorageCategory(StorageCategory.REFRIGERATED);
        cheese = ingredientRepository.save(cheese);

        Recipe tacos = new Recipe();
        tacos.setName("Beef Tacos");
        tacos.setBaseServings(4);
        tacos.setInstructions("Brown the beef. Assemble with cheese.");
        tacos.setIngredients(List.of(
                new RecipeIngredient(beef.getId(), beef.getName(),
                        new Measurement(new BigDecimal("1"), MeasurementUnit.LBS)),
                new RecipeIngredient(cheese.getId(), cheese.getName(),
                        new Measurement(new BigDecimal("2"), MeasurementUnit.CUP))
        ));

        Recipe saved = recipeRepository.save(tacos);
        assertNotNull(saved.getId());

        Recipe found = recipeRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Beef Tacos", found.getName());
        assertEquals(4, found.getBaseServings());
        assertEquals(2, found.getIngredients().size());
    }

    @Test
    void recipeSearchByName() {
        Recipe tacos = new Recipe();
        tacos.setName("Beef Tacos");
        tacos.setInstructions("Make tacos.");

        Recipe enchiladas = new Recipe();
        enchiladas.setName("Chicken Enchiladas");
        enchiladas.setInstructions("Make enchiladas.");

        recipeRepository.saveAll(List.of(tacos, enchiladas));

        List<Recipe> results = recipeRepository.findByNameContainingIgnoreCase("taco");
        assertEquals(1, results.size());
        assertEquals("Beef Tacos", results.get(0).getName());
    }

    @Test
    void referenceEndpointsReturnEnums() throws Exception {
        mockMvc.perform(get("/api/v1/reference/measurement-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("CUP")));

        mockMvc.perform(get("/api/v1/reference/grocery-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("PRODUCE")));

        mockMvc.perform(get("/api/v1/reference/storage-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("FROZEN")));
    }

    @Test
    void actuatorHealthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void recipeWithNestedIngredientsSurvivesMongoRoundTrip() {
        Recipe recipe = new Recipe();
        recipe.setName("Grilled Cheese");
        recipe.setInstructions("Butter bread, add cheese, grill.");
        recipe.setIngredients(List.of(
                new RecipeIngredient("i1", "Bread",
                        new Measurement(new BigDecimal("2"), MeasurementUnit.PIECE)),
                new RecipeIngredient("i2", "Butter",
                        new Measurement(new BigDecimal("1"), MeasurementUnit.TBSP)),
                new RecipeIngredient("i3", "American Cheese",
                        new Measurement(new BigDecimal("2"), MeasurementUnit.PIECE))
        ));
        Recipe saved = recipeRepository.save(recipe);

        Recipe found = recipeRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Grilled Cheese", found.getName());
        assertEquals(3, found.getIngredients().size());

        RecipeIngredient bread = found.getIngredients().get(0);
        assertEquals("Bread", bread.getIngredientName());
        assertEquals(0, new BigDecimal("2").compareTo(bread.getMeasurement().getQuantity()));
        assertEquals(MeasurementUnit.PIECE, bread.getMeasurement().getUnit());
    }

    // ===== HTTP-level CRUD tests =====

    @Test
    void createRecipeReturns201WithBody() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateRecipeRequest request = new CreateRecipeRequest("Beef Tacos",
                "Brown the beef. Assemble.", 4,
                List.of(
                        new RecipeIngredientRequest("ing1", "Ground Beef", new BigDecimal("1"), MeasurementUnit.LBS),
                        new RecipeIngredientRequest("ing2", "Cheddar Cheese", new BigDecimal("2"), MeasurementUnit.CUP)
                ));

        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Beef Tacos"))
                .andExpect(jsonPath("$.baseServings").value(4))
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.ingredients[0].ingredientName").value("Ground Beef"))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(1.00))
                .andExpect(jsonPath("$.ingredients[0].unit").value("LBS"));
    }

    @Test
    void createRecipeWithoutTokenReturns401() throws Exception {
        CreateRecipeRequest request = new CreateRecipeRequest("Tacos", "Cook.", 4, null);
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRecipeWithInvalidBodyReturns400() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"baseServings\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecipeByIdReturns200() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createRecipeViaApi(token, "Beef Tacos", 4);

        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Beef Tacos"))
                .andExpect(jsonPath("$.baseServings").value(4));
    }

    @Test
    void getRecipeFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String id = createRecipeViaApi(tokenA, "Secret Recipe", 2);

        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecipeWithServingsScalesQuantities() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateRecipeRequest request = new CreateRecipeRequest("Tacos", "Cook.", 4,
                List.of(new RecipeIngredientRequest("ing1", "Cheese", new BigDecimal("2"), MeasurementUnit.CUP)));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Scale from 4 to 8 servings — quantities should double
        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .param("servings", "8")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseServings").value(4))
                .andExpect(jsonPath("$.servings").value(8))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(4.00));
    }

    @Test
    void getRecipeScalingUsesCorrectRounding() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateRecipeRequest request = new CreateRecipeRequest("Soup", "Simmer.", 3,
                List.of(new RecipeIngredientRequest("ing1", "Broth", new BigDecimal("1"), MeasurementUnit.QUART)));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Scale from 3 to 7: 1 * (7/3) = 2.333... → 2.33 (HALF_UP)
        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .param("servings", "7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servings").value(7))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(2.33));
    }

    @Test
    void getRecipeWithoutServingsParamReturnsBaseQuantities() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateRecipeRequest request = new CreateRecipeRequest("Tacos", "Cook.", 4,
                List.of(new RecipeIngredientRequest("ing1", "Cheese", new BigDecimal("2"), MeasurementUnit.CUP)));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(2.00));
    }

    @Test
    void listRecipesScopedToOrg() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");

        createRecipeViaApi(tokenA, "Tacos", 4);
        createRecipeViaApi(tokenA, "Enchiladas", 6);
        createRecipeViaApi(tokenB, "Pasta", 2);

        mockMvc.perform(get("/api/v1/recipes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/recipes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listRecipesFilterByName() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        createRecipeViaApi(token, "Beef Tacos", 4);
        createRecipeViaApi(token, "Chicken Enchiladas", 6);

        mockMvc.perform(get("/api/v1/recipes")
                        .param("name", "taco")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Beef Tacos"));
    }

    @Test
    void updateRecipeReturns200() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createRecipeViaApi(token, "Taocs", 4);

        UpdateRecipeRequest update = new UpdateRecipeRequest("Tacos", "Updated instructions.", 6,
                List.of(new RecipeIngredientRequest("ing1", "Cheese", new BigDecimal("3"), MeasurementUnit.CUP)));

        mockMvc.perform(put("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tacos"))
                .andExpect(jsonPath("$.baseServings").value(6))
                .andExpect(jsonPath("$.instructions").value("Updated instructions."));
    }

    @Test
    void updateRecipeFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String id = createRecipeViaApi(tokenA, "Secret Recipe", 2);

        UpdateRecipeRequest update = new UpdateRecipeRequest("Stolen Recipe", "Ha.", 2, null);
        mockMvc.perform(put("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecipeReturns204ThenGetReturns404() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createRecipeViaApi(token, "Tacos", 4);

        mockMvc.perform(delete("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecipeFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String id = createRecipeViaApi(tokenA, "Secret Recipe", 2);

        mockMvc.perform(delete("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // -- helpers --

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

    private String createRecipeViaApi(String token, String name, int baseServings) throws Exception {
        CreateRecipeRequest request = new CreateRecipeRequest(name, "Instructions for " + name, baseServings,
                List.of(new RecipeIngredientRequest("ing1", "Ingredient", new BigDecimal("1"), MeasurementUnit.CUP)));
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
